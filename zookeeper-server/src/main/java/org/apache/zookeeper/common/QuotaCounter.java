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

 package org.apache.zookeeper.common;

 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicLong;
 import java.util.concurrent.atomic.AtomicReference;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 /**
  * A class that
  */
 public class QuotaCounter {

     /** Logger for this class */
     private static final Logger LOG = LoggerFactory.getLogger(QuotaCounter.class);

     private static final ConcurrentHashMap<String, Counter> stats = new ConcurrentHashMap<>();

     static class Counter {
         private AtomicLong bytes;
         private AtomicLong count;

         public AtomicLong getBytes() {
             return bytes;
         }

         public Counter(AtomicLong bytes, AtomicLong count) {
             this.bytes = bytes;
             this.count = count;
         }

         public AtomicLong getCount() {
             return count;
         }
     }

     public static long addAndGet(String key, long count) {
         return stats.computeIfAbsent(key, k ->
             new Counter(new AtomicLong(0L), new AtomicLong(0L)))
             .getCount()
             .addAndGet(count);
     }

     public static long getCount(String key) {
         return stats.computeIfAbsent(key, k ->
                 new Counter(new AtomicLong(0L), new AtomicLong(0L)))
                 .getCount()
                 .get();
     }
 }
