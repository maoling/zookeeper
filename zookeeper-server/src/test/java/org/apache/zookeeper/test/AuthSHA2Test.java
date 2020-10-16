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

import org.apache.zookeeper.server.auth.DigestStrategyFactory;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

public class AuthSHA2Test extends AuthTest {

    @BeforeEach
    public void setup() {
        // password is test
        System.setProperty("zookeeper.DigestAuthenticationProvider.digestAlg", DigestStrategyFactory.DigestAlgEnum.SHA_256.getName());
        System.setProperty("zookeeper.DigestAuthenticationProvider.superDigest", "super:wjySwxg860UATFtciuZ1lpzrCHrPeov6SPu/ZD56uig=");
        System.setProperty("zookeeper.authProvider.1", "org.apache.zookeeper.test.InvalidAuthProvider");
    }

    @Test
    public void testBadAuthNotifiesWatch() throws Exception {
        super.testBadAuthNotifiesWatch();
    }

    @Test
    public void testBadAuthThenSendOtherCommands() throws Exception {
        super.testBadAuthThenSendOtherCommands();
    }

    @Test
    public void testSuper() throws Exception {
        super.testSuper();
    }

    @Test
    public void testSuperACL() throws Exception {
        super.testSuperACL();
    }

    @Test
    public void testOrdinaryACL() throws Exception {
        super.testOrdinaryACL();
    }
}
