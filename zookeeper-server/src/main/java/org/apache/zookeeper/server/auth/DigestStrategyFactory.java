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

package org.apache.zookeeper.server.auth;

import static org.apache.zookeeper.server.auth.DigestStrategyFactory.DigestAlgEnum.SHA3_256;
import static org.apache.zookeeper.server.auth.DigestStrategyFactory.DigestAlgEnum.SHA_1;
import static org.apache.zookeeper.server.auth.DigestStrategyFactory.DigestAlgEnum.SHA_256;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;

public class DigestStrategyFactory {
    private static Map<String, DigestStrategy> digestStrategyMap = new HashMap<>();

    private DigestStrategyFactory() {
    }

    public enum DigestAlgEnum {
        SHA_1("SHA1"),
        SHA_256("SHA-256"),
        SHA3_256("SHA3-256");

        private String name;

        DigestAlgEnum(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static List<String> getValues() {
            List<String> digestList = new ArrayList<>();
            for (DigestAlgEnum digest : values()) {
                digestList.add(digest.getName());
            }
            return digestList;
        }
    }

    static {
        digestStrategyMap.put(SHA_1.getName(), new SHA1DigestStrategy());
        digestStrategyMap.put(SHA_256.getName(), new SHA256DigestStrategy());
        digestStrategyMap.put(SHA3_256.getName(), new SHA3256DigestStrategy());
    }

    public static DigestStrategy getInstance(String digestAlgorithm) {
        return digestStrategyMap.get(digestAlgorithm);
    }

    static class SHA1DigestStrategy implements DigestStrategy {

        @Override
        public byte[] generateDigest(String idPassword) throws NoSuchAlgorithmException {
            return MessageDigest.getInstance(SHA_1.getName()).digest(idPassword.getBytes());
        }
    }

    static class SHA256DigestStrategy implements DigestStrategy {

        @Override
        public byte[] generateDigest(String idPassword) throws NoSuchAlgorithmException {
            return MessageDigest.getInstance(SHA_256.getName()).digest(idPassword.getBytes());
        }
    }

    static class SHA3256DigestStrategy implements DigestStrategy {

        @Override
        public byte[] generateDigest(String idPassword) {
            byte[] bytes = idPassword.getBytes();
            Digest digest = new SHA3Digest(256);
            digest.update(bytes, 0, bytes.length);
            byte[] digestByte = new byte[digest.getDigestSize()];
            digest.doFinal(digestByte, 0);
            return digestByte;
        }
    }
}


