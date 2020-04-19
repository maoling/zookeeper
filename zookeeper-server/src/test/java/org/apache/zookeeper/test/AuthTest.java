/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.test;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.TestableZooKeeper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AuthTest extends ClientBase {

    @Before
    public void setup() {
        // password is test
        System.setProperty("zookeeper.DigestAuthenticationProvider.superDigest", "super:D/InIHSb7yEEbrWz8b9l71RjZJU=");
        System.setProperty("zookeeper.authProvider.1", "org.apache.zookeeper.test.InvalidAuthProvider");
    }

    @After
    public void teardown() {
        System.clearProperty("zookeeper.DigestAuthenticationProvider.superDigest");
        System.clearProperty("zookeeper.DigestAuthenticationProvider.digestAlg");
    }

    protected final CountDownLatch authFailed = new CountDownLatch(1);

    @Override
    protected TestableZooKeeper createClient(String hp) throws IOException, InterruptedException {
        MyWatcher watcher = new MyWatcher();
        return createClient(watcher, hp);
    }

    private class MyWatcher extends CountdownWatcher {

        @Override
        public synchronized void process(WatchedEvent event) {
            if (event.getState() == KeeperState.AuthFailed) {
                authFailed.countDown();
            } else {
                super.process(event);
            }
        }

    }

    @Test
    public void testBadAuthNotifiesWatch() throws Exception {
        ZooKeeper zk = createClient();
        try {
            zk.addAuthInfo("FOO", "BAR".getBytes());
            zk.getData("/path1", false, null);
            fail("Should get auth state error");
        } catch (KeeperException.AuthFailedException e) {
            if (!authFailed.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Should have called my watcher");
            }
        } finally {
            zk.close();
        }
    }

    @Test
    public void testBadAuthThenSendOtherCommands() throws Exception {
        ZooKeeper zk = createClient();
        try {
            zk.addAuthInfo("INVALID", "BAR".getBytes());
            zk.exists("/foobar", false);
            zk.getData("/path1", false, null);
            fail("Should get auth state error");
        } catch (KeeperException.AuthFailedException e) {
            if (!authFailed.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                fail("Should have called my watcher");
            }
        } finally {
            zk.close();
        }
    }

    @Test
    public void testSuper() throws Exception {
        ZooKeeper zk = createClient();
        try {
            zk.addAuthInfo("digest", "pat:pass".getBytes());
            zk.create("/path1", null, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
            zk.close();
            // verify no auth
            zk = createClient();
            try {
                zk.getData("/path1", false, null);
                fail("auth verification");
            } catch (KeeperException.NoAuthException e) {
                // expected
            }
            zk.close();
            // verify bad pass fails
            zk = createClient();
            zk.addAuthInfo("digest", "pat:pass2".getBytes());
            try {
                zk.getData("/path1", false, null);
                fail("auth verification");
            } catch (KeeperException.NoAuthException e) {
                // expected
            }
            zk.close();
            // verify super with bad pass fails
            zk = createClient();
            zk.addAuthInfo("digest", "super:test2".getBytes());
            try {
                zk.getData("/path1", false, null);
                fail("auth verification");
            } catch (KeeperException.NoAuthException e) {
                // expected
            }
            zk.close();
            // verify super with correct pass success
            zk = createClient();
            zk.addAuthInfo("digest", "super:test".getBytes());
            zk.getData("/path1", false, null);
        } finally {
            zk.close();
        }
    }

    @Test
    public void testSuperACL() throws Exception {
        ZooKeeper zk = createClient();
        try {
            zk.addAuthInfo("digest", "pat:pass".getBytes());
            zk.create("/path1", null, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);
            zk.close();
            // verify super can do anything and ignores ACLs
            zk = createClient();
            zk.addAuthInfo("digest", "super:test".getBytes());
            zk.getData("/path1", false, null);

            zk.setACL("/path1", Ids.READ_ACL_UNSAFE, -1);
            zk.create("/path1/foo", null, Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT);

            zk.setACL("/path1", Ids.OPEN_ACL_UNSAFE, -1);

        } finally {
            zk.close();
        }
    }

    @Test
    public void testOrdinaryACL() throws Exception {
        ZooKeeper zk = createClient();
        try {
            String path = "/path1";
            zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zk.addAuthInfo("digest", "username1:password1".getBytes());
            List<ACL> list = new ArrayList<>();
            int perm = ZooDefs.Perms.ALL;
            String userPassword = "username1:password1";
            Id id = new Id("auth", userPassword);
            list.add(new ACL(perm, id));
            zk.setACL(path, list, -1);
            zk.close();

            zk = createClient();
            zk.addAuthInfo("digest", "super:test".getBytes());
            zk.getData(path, false, null);
            zk.close();

            zk = createClient();
            try {
                zk.getData(path, false, null);
                fail("should have NoAuthException");
            } catch (KeeperException.NoAuthException e) {
                // expected
            }
            zk.addAuthInfo("digest", "username1:password1".getBytes());
            zk.getData(path, false, null);
        } finally {
            zk.close();
        }
    }

}
