/**
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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Quotas;
import org.apache.zookeeper.StatsTrack;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeperMain;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.cli.ListQuotaCommand;
import org.apache.zookeeper.cli.MalformedPathException;
import org.apache.zookeeper.cli.SetQuotaCommand;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.ContainerManager;
import org.apache.zookeeper.server.DataNode;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.Assert;
import org.junit.Test;

public class ZooKeeperQuotaTest extends ClientBase {

    @Test
    public void testQuota() throws IOException,
        InterruptedException, KeeperException, Exception {
        final ZooKeeper zk = createClient();
        final String path = "/a/b/v";
        // making sure setdata works on /
        zk.setData("/", "some".getBytes(), -1);
        zk.create("/a", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b/v", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);

        zk.create("/a/b/v/d", "some".getBytes(), Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);
        ZooKeeperMain.createQuota(zk, path, 5L, 10);

        // see if its set
        String absolutePath = Quotas.quotaZookeeper + path + "/" + Quotas.limitNode;
        byte[] data = zk.getData(absolutePath, false, new Stat());
        StatsTrack st = new StatsTrack(new String(data));
        Assert.assertTrue("bytes are set", st.getBytes() == 5L);
        Assert.assertTrue("num count is set", st.getCount() == 10);

        String statPath = Quotas.quotaZookeeper + path + "/" + Quotas.statNode;
        byte[] qdata = zk.getData(statPath, false, new Stat());
        StatsTrack qst = new StatsTrack(new String(qdata));
        Assert.assertTrue("bytes are set", qst.getBytes() == 8L);
        Assert.assertTrue("count is set", qst.getCount() == 2);

        //force server to restart and load from snapshot, not txn log
        stopServer();
        startServer();
        stopServer();
        startServer();
        ZooKeeperServer server = serverFactory.getZooKeeperServer();
        Assert.assertNotNull("Quota is still set",
            server.getZKDatabase().getDataTree().getMaxPrefixWithQuota(path) != null);
    }

    /**
     * ZOOKEEPER-2559
     */
    @Test
    public void testQuotaShouldNotExistWhenOriginalPathDeleted()
            throws KeeperException, InterruptedException, IOException {
        final ZooKeeper zk = createClient();
        String path = "/foo";
        zk.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        //set the quota
        try {
            SetQuotaCommand.createQuota(zk, path, 8, 8);
        } catch (MalformedPathException e) {
            e.printStackTrace();
        }
        // delete the Original Path
        zk.delete(path, -1);

        //Way-1: let ContainerManager clean the quotaPath periodically。
        final AtomicLong fakeElapsed = new AtomicLong(0);
        ContainerManager containerManager = newContainerManager(fakeElapsed);
        containerManager.checkContainers();
        Assert.assertNull("node should have been deleted", zk.exists(Quotas.quotaZookeeper + path, false));

        //Way-2:let the listQuota() clean the quotaPath explicitly.
        path = "/bar";
        zk.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        //set the quota
        try {
            SetQuotaCommand.createQuota(zk, path, 8, 8);
        } catch (MalformedPathException e) {
            e.printStackTrace();
        }
        // delete the Original Path
        zk.delete(path, -1);
        ListQuotaCommand listQuotaCommand = new ListQuotaCommand();
        // list the quota
        listQuotaCommand.listQuota(zk, path);
        Assert.assertNull("node should have been deleted", zk.exists(Quotas.quotaZookeeper + path, false));
    }

    private ContainerManager newContainerManager(final AtomicLong fakeElapsed) {
        return new ContainerManager(serverFactory.getZooKeeperServer()
                .getZKDatabase(), serverFactory.getZooKeeperServer().firstProcessor, 1, 100) {
            @Override
            protected long getElapsed(DataNode node) {
                return fakeElapsed.get();
            }
        };
    }
}
