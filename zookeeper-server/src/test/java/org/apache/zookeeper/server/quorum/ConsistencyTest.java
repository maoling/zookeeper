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

package org.apache.zookeeper.server.quorum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.test.ClientBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A series of unit tests to check the consistency semantics for ZooKeeper
 */
public class ConsistencyTest extends QuorumPeerTestBase {
    protected static final Logger LOG = LoggerFactory.getLogger(ConsistencyTest.class);

    @Test
    @Timeout(value = 120)
    public void testSequentialConsistencyNormal() throws Exception {
        long start = System.currentTimeMillis();
        int numServers = 3;
        Servers srvs = LaunchServers(numServers, 0, null);
        String connectString = getConnectString(srvs);

        ZooKeeper client = ClientBase.createZKClient(connectString);
        String path = "/testSequentialConsistencyNormal";
        AtomicLong logicClock = new AtomicLong(0);
        String data = String.valueOf(logicClock.get());
        client.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        byte[] data1 = client.getData(path, false, null);
        assertEquals(data, new String(data1));
        client.close();

        int clientThreads = 20;
        ConcurrentHashMap<Integer, List<Integer>> historyMap = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(clientThreads);
        for (int i = 0; i < clientThreads / 2; i++) {
            executorService.execute(new WriteClientThread(i, connectString, path, logicClock));
        }
        for (int i = 0; i < clientThreads / 2; i++) {
            executorService.execute(new ReadClientThread(i, connectString, path, logicClock, historyMap));
        }
        shutdownAndAwaitTermination(executorService);
        for (Map.Entry<Integer, List<Integer>> entry : historyMap.entrySet()) {
            List<Integer> list = entry.getValue();
            LOG.info("clientId:{}, list:{}", entry.getKey(), list);
            if (list.size() == 0) {
               continue;
            }
            for (int i = 1; i < list.size(); i++) {
                assertTrue(list.get(i) >= list.get(i - 1), String.format("value:%d should great than or equal value: %d",
                            list.get(i), list.get(i - 1)));
            }
        }
        // clean up
        srvs.shutDownAllServers();
        for (ZooKeeper zk : srvs.zk) {
            zk.close();
        }
        LOG.info("spent time:{} ms", System.currentTimeMillis() - start);
    }

    void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    LOG.warn("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private String getConnectString(Servers srvs) {
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < srvs.clientPorts.length; index++) {
            int clientPort = srvs.clientPorts[index];
            if (index == 0) {
                sb.append("127.0.0.1:" + clientPort);
            } else {
                sb.append(",127.0.0.1:" + clientPort);
            }
        }
        return sb.toString();
    }

    static class ReadClientThread implements Runnable {
        int clientId;
        String connectString;
        AtomicLong logicClock;
        String path;
        ConcurrentHashMap<Integer, List<Integer>> historyMap;

        public ReadClientThread(int clientId, String connectString, String path,
                AtomicLong logicClock,
                ConcurrentHashMap<Integer, List<Integer>> historyMap) {
            this.clientId = clientId;
            this.connectString = connectString;
            this.logicClock = logicClock;
            this.path = path;
            this.historyMap = historyMap;
        }

        @Override
        public void run() {
            ZooKeeper client = null;
            try {
                Thread.currentThread().setName("Read-Thread-" + clientId);
                client = ClientBase.createZKClient(connectString);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.close();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return;
            }

            try {
                while (logicClock.get() <= 10000) {
                    String data2Str = new String(client.getData(path, false, null));
                    Integer data2 = Integer.parseInt(data2Str);
                    LOG.info("ThreadName:{}, read data:{}", Thread.currentThread().getName(), data2Str);
                    historyMap.computeIfAbsent(clientId, k -> new ArrayList<>()).add(data2);
                    //assertTrue(data2 <= data1, String.format("data2: %d value should less than or equal data1: %d",
                    // data2, data1));
                }
            } catch (InterruptedException | KeeperException e) {
                e.printStackTrace();
            } finally {
                try {
                    client.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class WriteClientThread implements Runnable {
        int clientId;
        String connectString;
        AtomicLong logicClock;
        String path;

        public WriteClientThread(int clientId, String connectString, String path, AtomicLong logicClock) {
            this.clientId = clientId;
            this.connectString = connectString;
            this.logicClock = logicClock;
            this.path = path;
        }

        @Override
        public void run() {
            ZooKeeper client = null;
            try {
                Thread.currentThread().setName("Write-Thread-" + clientId);
                client = ClientBase.createZKClient(connectString);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    client.close();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return;
            }

            try {
                while (logicClock.get() <= 10000) {
                    synchronized (logicClock) {
                        String data1Str = String.valueOf(logicClock.incrementAndGet());
                        client.setData(path, data1Str.getBytes(), -1);
                        LOG.info("ThreadName:{}, write data:{}", Thread.currentThread().getName(), data1Str);
                    }
                }
            } catch (InterruptedException | KeeperException e) {
                e.printStackTrace();
            } finally {
                try {
                    client.close();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // See ZOOKEEPER-3875
    @Test
    @Timeout(value = 120)
    public void testSequentialConsistency() throws Exception {
        int srvA = 0;
        int srvB = 1;
        int srvC = 2;
        int numServers = 3;
        Servers srvs = LaunchServers(numServers);

        // 0. Initialize the cluster: initially /key0 == 0 and /key1 == 0
        String[] keys = {"/key0", "/key1"};
        srvs.zk[srvC].multi(Arrays.asList(
                Op.create(keys[0], "0".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT),
                Op.create(keys[1], "0".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
        ));
        srvs.shutDownAllServers();
        waitForAll(srvs.zk, States.CONNECTING);

        // 1. Start A, B
        srvs.mt[srvA].start();
        srvs.mt[srvB].start();
        waitForAll(new ZooKeeper[] {srvs.zk[srvA], srvs.zk[srvB]}, States.CONNECTED);

        // Crash A and initiate write on B: /key1 = 101
        srvs.mt[srvA].shutdown();
        srvs.zk[srvB].setData(keys[1], "101".getBytes(), -1, null, null);
        Thread.sleep(1000);

        // Stop B
        srvs.mt[srvB].shutdown();
        waitForAll(new ZooKeeper[] {srvs.zk[srvA], srvs.zk[srvB]}, States.CONNECTING);

        // 2. Start and stop A, B
        srvs.mt[srvA].start();
        srvs.mt[srvB].start();
        waitForAll(new ZooKeeper[] {srvs.zk[srvA], srvs.zk[srvB]}, States.CONNECTED);
        srvs.mt[srvA].shutdown();
        srvs.mt[srvB].shutdown();
        waitForAll(new ZooKeeper[] {srvs.zk[srvA], srvs.zk[srvB]}, States.CONNECTING);

        // 3. Start A, C
        srvs.mt[srvA].start();
        srvs.mt[srvC].start();
        waitForAll(new ZooKeeper[] {srvs.zk[srvA], srvs.zk[srvC]}, States.CONNECTED);

        // Initiate conditional write on A: if (/key1 == 101): /key0 = 200
        srvs.zk[srvA].getData(keys[1], false, (returnCode, key, ctx, result, stat) -> {
            if (returnCode == KeeperException.Code.OK.intValue() && Arrays.equals(result, "101".getBytes())) {
                srvs.zk[srvA].setData(keys[0], "200".getBytes(), -1, null, null);
            }
        }, null);
        Thread.sleep(1000);

        // Stop A, C
        srvs.mt[srvA].shutdown();
        srvs.mt[srvC].shutdown();
        waitForAll(new ZooKeeper[] {srvs.zk[srvA], srvs.zk[srvC]}, States.CONNECTING);

        // 4. Start A, B, C
        srvs.mt[srvA].start();
        srvs.mt[srvB].start();
        srvs.mt[srvC].start();
        waitForAll(srvs.zk, States.CONNECTED);

        // Initiate conditional write on B: if (/key1 == 0): /key1 = 301
        srvs.zk[srvB].getData(keys[1], false, (returnCode, key, ctx, result, stat) -> {
            if (returnCode == KeeperException.Code.OK.intValue() && Arrays.equals(result, "0".getBytes())) {
                srvs.zk[srvB].setData(keys[1], "301".getBytes(), -1, null, null);
            }
        }, null);
        Thread.sleep(1000);

        // Stop A, B, C
        srvs.shutDownAllServers();
        waitForAll(srvs.zk, States.CONNECTING);

        // Start all servers and check that their state is allowed under sequential consistency
        srvs.mt[srvA].start();
        srvs.mt[srvB].start();
        srvs.mt[srvC].start();
        waitForAll(srvs.zk, States.CONNECTED);

        int[][] values = new int[2][numServers];
        for (int srv = 0; srv < numServers; srv++) {
            byte[] rawValue0 = srvs.zk[srv].getData(keys[0], false, null);
            values[0][srv] = Integer.parseInt(new String(rawValue0));
            LOG.info("Value for /key0 on server {}: {}", srv, values[0][srv]);

            byte[] rawValue1 = srvs.zk[srv].getData(keys[1], false, null);
            values[1][srv] = Integer.parseInt(new String(rawValue1));
            LOG.info("Value for /key1 on server {}: {}", srv, values[1][srv]);
        }

        assertTrue(values[0][0] == values[0][1] && values[0][0] == values[0][2],
                "Values associated with /key0 should be equal on all servers");
        assertTrue(values[1][0] == values[1][1] && values[1][0] == values[1][2],
                "Values associated with /key1 should be equal on all servers");

        List<Integer> finalValues = Arrays.asList(values[0][0], values[1][0]);

        // Values associated with /key0 and /key1 that are allowed under sequential consistency,
        // where we assume that any of the write requests may have failed.
        Set<List<Integer>> allowedValues = new HashSet<>();
        allowedValues.add(Arrays.asList(0, 0));
        allowedValues.add(Arrays.asList(0, 101));
        allowedValues.add(Arrays.asList(200, 101));
        allowedValues.add(Arrays.asList(0, 301));

        assertTrue(allowedValues.contains(finalValues),
                "Final values of /key0 and /key1 should be allowed under sequential consistency");

        // clean up
        srvs.shutDownAllServers();
        for (ZooKeeper zk : srvs.zk) {
            zk.close();
        }
    }

    // See ZOOKEEPER-2832
    @Test
    @Timeout(value = 120)
    public void testDivergenceResync() throws Exception {
        int numKeys = 2;
        String[] keys = new String[numKeys];
        int base;

        byte[][] outputA = new byte[4][];
        byte[][] outputB = new byte[4][];
        byte[][] outputC = new byte[4][];
        String outputAKey1 = null;
        String outputBKey1 = null;
        String outputCKey1 = null;
        String outputAKey2 = null;
        String outputBKey2 = null;
        String outputCKey2 = null;

        int numServers = 3;
        Servers svrs = LaunchServers(numServers);

        String path = "/testDivergenceResync";
        int srvA = -1;
        int srvB = -1;
        int srvC = -1;

        for (int i = 0; i < numKeys; i++) {
            keys[i] = path + i;
        }

        /** 0. Initialization */
        // find the leader
        for (int i = 0; i < numServers; i++) {
            if (svrs.mt[i].main.quorumPeer.leader != null) {
                srvC = i;
            }
        }

        // make sure there is a leader
        assertTrue(srvC >= 0, "There should be a leader");

        base = 0;

        // create initial znodes
        for (int j = 0; j < numKeys; j++) {
            byte[] valToWrite = ((j + base) + "").getBytes();
            svrs.zk[srvC].create(keys[j], valToWrite, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        base = 1000;

        srvA = (srvC + 1) % numServers;
        srvB = (srvC + 2) % numServers;

        // Shutdown A, B, C
        svrs.mt[srvA].shutdown();
        svrs.mt[srvB].shutdown();
        svrs.mt[srvC].shutdown();
        waitForAll(svrs.zk, States.CONNECTING);

        // Resync A and B
        svrs.mt[srvA].start();
        svrs.mt[srvB].start();
        ZooKeeper[] resyncNodes = new ZooKeeper[2];
        resyncNodes[0] = svrs.zk[srvA];
        resyncNodes[1] = svrs.zk[srvB];
        waitForAll(resyncNodes, States.CONNECTED);
        int leader = -1;
        for (int i = 0; i < numServers; i++) {
            if (svrs.mt[i].main.quorumPeer.leader != null) {
                leader = i;
            }
        }
        assertTrue(leader >= 0, "There should be a leader");
        assertTrue(leader == srvB, "There should be a leader");

        // Divergence
        svrs.mt[srvA].shutdown();
        byte[] valToWrite = ((0 + base) + "").getBytes();
        svrs.zk[srvB].setData(keys[0], valToWrite, -1, null, null);
        Thread.sleep(1000);
        System.gc();
        svrs.mt[srvB].shutdown();


        // Start and shutdown A and C
        svrs.mt[srvA].start();
        svrs.mt[srvC].start();
        resyncNodes = new ZooKeeper[2];
        resyncNodes[0] = svrs.zk[srvA];
        resyncNodes[1] = svrs.zk[srvC];
        waitForAll(resyncNodes, States.CONNECTED);
        leader = -1;
        for (int i = 0; i < numServers; i++) {
            if (svrs.mt[i].main.quorumPeer.leader != null) {
                leader = i;
            }
        }
        assertTrue(leader >= 0, "There should be a leader");
        assertTrue(leader == srvA, "There should be a leader");
        svrs.mt[srvA].shutdown();
        svrs.mt[srvC].shutdown();
        waitForAll(svrs.zk, States.CONNECTING);

        // Resync B and C
        svrs.mt[srvB].start();
        svrs.mt[srvC].start();
        resyncNodes = new ZooKeeper[2];
        resyncNodes[0] = svrs.zk[srvB];
        resyncNodes[1] = svrs.zk[srvC];
        waitForAll(resyncNodes, States.CONNECTED);
        leader = -1;
        for (int i = 0; i < numServers; i++) {
            if (svrs.mt[i].main.quorumPeer.leader != null) {
                leader = i;
            }
        }
        assertTrue(leader >= 0, "There should be a leader");
        assertTrue(leader == srvC, "There should be a leader");

        // Divergence
        valToWrite = ((1 + base) + "").getBytes();
        svrs.zk[srvC].setData(keys[1], valToWrite, -1, null, null);
        Thread.sleep(1000);
        System.gc();
        svrs.mt[srvB].shutdown();
        svrs.mt[srvC].shutdown();
        waitForAll(svrs.zk, States.CONNECTING);

        // Resync B and C
        svrs.mt[srvB].start();
        svrs.mt[srvC].start();
        resyncNodes = new ZooKeeper[2];
        resyncNodes[0] = svrs.zk[srvB];
        resyncNodes[1] = svrs.zk[srvC];
        waitForAll(resyncNodes, States.CONNECTED);
        leader = -1;
        for (int i = 0; i < numServers; i++) {
            if (svrs.mt[i].main.quorumPeer.leader != null) {
                leader = i;
            }
        }
        assertTrue(leader >= 0, "There should be a leader");
        assertTrue(leader == srvC, "There should be a leader");

        // Restart A as well
        svrs.mt[srvA].start();
        resyncNodes = new ZooKeeper[1];
        resyncNodes[0] = svrs.zk[srvA];
        waitForAll(resyncNodes, States.CONNECTED);
        leader = -1;
        for (int i = 0; i < numServers; i++) {
            if (svrs.mt[i].main.quorumPeer.leader != null) {
                leader = i;
            }
        }
        assertTrue(leader >= 0, "There should be a leader");
        assertTrue(leader == srvC, "There should be a leader");


        /** invariant check */
        outputA[0] = svrs.zk[srvA].getData(path + 0, false, null);
        outputA[1] = svrs.zk[srvA].getData(path + 1, false, null);
        outputB[0] = svrs.zk[srvB].getData(path + 0, false, null);
        outputB[1] = svrs.zk[srvB].getData(path + 1, false, null);
        outputC[0] = svrs.zk[srvC].getData(path + 0, false, null);
        outputC[1] = svrs.zk[srvC].getData(path + 1, false, null);

        outputAKey1 = new String(outputA[0], "UTF-8");
        outputBKey1 = new String(outputB[0], "UTF-8");
        outputCKey1 = new String(outputC[0], "UTF-8");
        outputAKey2 = new String(outputA[1], "UTF-8");
        outputBKey2 = new String(outputB[1], "UTF-8");
        outputCKey2 = new String(outputC[1], "UTF-8");

        LOG.info(
                "Output results outputAKey1={}, outputBKey1={}, outputCKey1={}; outputAKey2={}, outputBKey2={}, "
                        + "outputCKey2={}",
                outputAKey1, outputBKey1, outputCKey1, outputAKey2, outputBKey2, outputCKey2);

        assertEquals(outputAKey1, outputBKey1,
                "Expecting the value of the 1st key on 1st and 2nd servers should be same");
        assertEquals(outputBKey1, outputCKey1,
                "Expecting the value of the 1st key on 2nd and 3rd servers should be same");
        assertEquals(outputAKey2, outputBKey2,
                "Expecting the value of the 2nd key on 1st and 2nd servers should be same");
        assertEquals(outputBKey2, outputCKey2,
                "Expecting the value of the 2nd key on 2nd and 3rd servers should be same");
        // clean up
        svrs.shutDownAllServers();
        for (ZooKeeper zk : svrs.zk) {
            zk.close();
        }

    }
}
