//package org.apache.zookeeper.server;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.Map;
//
//public class LFUCache<K, V> extends HashMap<K, V> {
//    int minFreq, capacity;
//    Map<K, Node> keyTable;
//    Map<Integer, LinkedList<Node>> freqTable;
//
//    public LFUCache(int capacity) {
//        this.minFreq = 0;
//        this.capacity = capacity;
//        keyTable = new HashMap<>();
//        freqTable = new HashMap<>();
//    }
//
//    @Override
//    public V get(Object key) {
//        if (capacity == 0) {
//            return null;
//        }
//        if (!keyTable.containsKey(key)) {
//            return null;
//        }
//        Node node = keyTable.get(key);
//        V val = (V) node.val;
//        int freq = node.freq;
//        freqTable.get(freq).remove(node);
//        // 如果当前链表为空，我们需要在哈希表中删除，且更新minFreq
//        if (freqTable.get(freq).size() == 0) {
//            freqTable.remove(freq);
//            if (minFreq == freq) {
//                minFreq += 1;
//            }
//        }
//        // 插入到 freq + 1 中
//        LinkedList<Node> list = freqTable.getOrDefault(freq + 1, new LinkedList<Node>());
//        list.offerFirst(new Node(key, val, freq + 1));
//        freqTable.put(freq + 1, list);
//        keyTable.put((K) key, freqTable.get(freq + 1).peekFirst());
//        return val;
//    }
//
//    @Override
//    public V put(K key, V value) {
//        if (capacity == 0) {
//            return null;
//        }
//        if (!keyTable.containsKey(key)) {
//            // 缓存已满，需要进行删除操作
//            if (keyTable.size() == capacity) {
//                // 通过 minFreq 拿到 freq_table[minFreq] 链表的末尾节点
//                Node node = freqTable.get(minFreq).peekLast();
//                keyTable.remove(node.key);
//                freqTable.get(minFreq).pollLast();
//                if (freqTable.get(minFreq).size() == 0) {
//                    freqTable.remove(minFreq);
//                }
//            }
//            LinkedList<Node> list = freqTable.getOrDefault(1, new LinkedList<Node>());
//            list.offerFirst(new Node(key, value, 1));
//            freqTable.put(1, list);
//            keyTable.put(key, freqTable.get(1).peekFirst());
//            minFreq = 1;
//        } else {
//            // 与 get 操作基本一致，除了需要更新缓存的值
//            Node node = keyTable.get(key);
//            int freq = node.freq;
//            freqTable.get(freq).remove(node);
//            if (freqTable.get(freq).size() == 0) {
//                freqTable.remove(freq);
//                if (minFreq == freq) {
//                    minFreq += 1;
//                }
//            }
//            LinkedList<Node> list = freqTable.getOrDefault(freq + 1, new LinkedList<Node>());
//            list.offerFirst(new Node(key, value, freq + 1));
//            freqTable.put(freq + 1, list);
//            keyTable.put(key, freqTable.get(freq + 1).peekFirst());
//        }
//        //return value;
//        return value;
//    }
//
//    @Override
//    public V remove(Object key) {
//        System.out.println("LFU don't implement remove method!!!! fuck");
//        return null;
//    }
//}
//
//class Node<K, V> {
//    K  key;
//    V val;
//    int freq;
//
//    Node(K key, V val, int freq) {
//        this.key = key;
//        this.val = val;
//        this.freq = freq;
//    }
//}