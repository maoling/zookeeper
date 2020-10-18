///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.zookeeper.test;
//
//import java.security.NoSuchAlgorithmException;
//import org.apache.zookeeper.server.auth.DigestStrategy;
//import org.apache.zookeeper.server.auth.DigestStrategyFactory;
//import org.junit.Assert;
//import org.junit.Test;
//
//public class DigestAuthenticationProviderTest extends ClientBase {
//
//    @Test
//    public void testSHA_1() throws NoSuchAlgorithmException {
//        DigestStrategy strategy = DigestStrategyFactory.getInstance(DigestStrategyFactory.DigestAlgEnum.SHA_1.getName());
//
//        Assert.assertEquals("8a0444ded963cf1118dd34aa1acaafec268c654d", getGeneratedDigestStr(strategy.generateDigest("zookeeper")));
//        Assert.assertEquals("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", getGeneratedDigestStr(strategy.generateDigest("foo")));
//        Assert.assertEquals("62cdb7020ff920e5aa642c3d4066950dd1f01f4d", getGeneratedDigestStr(strategy.generateDigest("bar")));
//    }
//
//    @Test
//    public void testSHA_256() throws NoSuchAlgorithmException {
//        DigestStrategy strategy = DigestStrategyFactory.getInstance(DigestStrategyFactory.DigestAlgEnum.SHA_256.getName());
//
//        Assert.assertEquals("456831beef3fc1500939995d7369695f48642664a02d5eab9d807592a08b2384", getGeneratedDigestStr(strategy.generateDigest("zookeeper")));
//        Assert.assertEquals("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", getGeneratedDigestStr(strategy.generateDigest("foo")));
//        Assert.assertEquals("fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9", getGeneratedDigestStr(strategy.generateDigest("bar")));
//    }
//
//    @Test
//    public void testSHA3_256() throws NoSuchAlgorithmException {
//        DigestStrategy strategy = DigestStrategyFactory.getInstance(DigestStrategyFactory.DigestAlgEnum.SHA3_256.getName());
//
//        Assert.assertEquals("af4c1abc2deaa6edffc7ce34edeb8c03ee9a1488b64fd318ddb93b4b7f1c0746", getGeneratedDigestStr(strategy.generateDigest("zookeeper")));
//        Assert.assertEquals("76d3bc41c9f588f7fcd0d5bf4718f8f84b1c41b20882703100b9eb9413807c01", getGeneratedDigestStr(strategy.generateDigest("foo")));
//        Assert.assertEquals("cceefd7e0545bcf8b6d19f3b5750c8a3ee8350418877bc6fb12e32de28137355", getGeneratedDigestStr(strategy.generateDigest("bar")));
//    }
//
//    private static String getGeneratedDigestStr(byte[] bytes) {
//        StringBuilder stringBuilder = new StringBuilder("");
//        if (bytes == null || bytes.length <= 0) {
//            return null;
//        }
//        for (int i = 0; i < bytes.length; i++) {
//            int v = bytes[i] & 0xFF;
//            String hv = Integer.toHexString(v);
//            if (hv.length() < 2) {
//                stringBuilder.append(0);
//            }
//            stringBuilder.append(hv);
//        }
//        return stringBuilder.toString();
//    }
//}/*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.apache.zookeeper.test;
//
//import java.security.NoSuchAlgorithmException;
//import org.apache.zookeeper.server.auth.DigestStrategy;
//import org.apache.zookeeper.server.auth.DigestStrategyFactory;
//import org.junit.Assert;
//import org.junit.Test;
//
//public class DigestAuthenticationProviderTest extends ClientBase {
//
//    @Test
//    public void testSHA_1() throws NoSuchAlgorithmException {
//        DigestStrategy strategy = DigestStrategyFactory.getInstance(DigestStrategyFactory.DigestAlgEnum.SHA_1.getName());
//
//        Assert.assertEquals("8a0444ded963cf1118dd34aa1acaafec268c654d", getGeneratedDigestStr(strategy.generateDigest("zookeeper")));
//        Assert.assertEquals("0beec7b5ea3f0fdbc95d0dd47f3c5bc275da8a33", getGeneratedDigestStr(strategy.generateDigest("foo")));
//        Assert.assertEquals("62cdb7020ff920e5aa642c3d4066950dd1f01f4d", getGeneratedDigestStr(strategy.generateDigest("bar")));
//    }
//
//    @Test
//    public void testSHA_256() throws NoSuchAlgorithmException {
//        DigestStrategy strategy = DigestStrategyFactory.getInstance(DigestStrategyFactory.DigestAlgEnum.SHA_256.getName());
//
//        Assert.assertEquals("456831beef3fc1500939995d7369695f48642664a02d5eab9d807592a08b2384", getGeneratedDigestStr(strategy.generateDigest("zookeeper")));
//        Assert.assertEquals("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", getGeneratedDigestStr(strategy.generateDigest("foo")));
//        Assert.assertEquals("fcde2b2edba56bf408601fb721fe9b5c338d10ee429ea04fae5511b68fbf8fb9", getGeneratedDigestStr(strategy.generateDigest("bar")));
//    }
//
//    @Test
//    public void testSHA3_256() throws NoSuchAlgorithmException {
//        DigestStrategy strategy = DigestStrategyFactory.getInstance(DigestStrategyFactory.DigestAlgEnum.SHA3_256.getName());
//
//        Assert.assertEquals("af4c1abc2deaa6edffc7ce34edeb8c03ee9a1488b64fd318ddb93b4b7f1c0746", getGeneratedDigestStr(strategy.generateDigest("zookeeper")));
//        Assert.assertEquals("76d3bc41c9f588f7fcd0d5bf4718f8f84b1c41b20882703100b9eb9413807c01", getGeneratedDigestStr(strategy.generateDigest("foo")));
//        Assert.assertEquals("cceefd7e0545bcf8b6d19f3b5750c8a3ee8350418877bc6fb12e32de28137355", getGeneratedDigestStr(strategy.generateDigest("bar")));
//    }
//
//    private static String getGeneratedDigestStr(byte[] bytes) {
//        StringBuilder stringBuilder = new StringBuilder("");
//        if (bytes == null || bytes.length <= 0) {
//            return null;
//        }
//        for (int i = 0; i < bytes.length; i++) {
//            int v = bytes[i] & 0xFF;
//            String hv = Integer.toHexString(v);
//            if (hv.length() < 2) {
//                stringBuilder.append(0);
//            }
//            stringBuilder.append(hv);
//        }
//        return stringBuilder.toString();
//    }
//}