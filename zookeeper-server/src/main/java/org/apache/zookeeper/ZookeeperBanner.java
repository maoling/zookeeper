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

package org.apache.zookeeper;

/**
 * ZookeeperBanner which writes the 'ZooKeeper' banner at the start of zk server.
 *
 */
public class ZookeeperBanner {

    private static final String[] BANNER = {"",
        "  ______              _  __                              \n" ,
        " |___  /             | |/ /                              \n" ,
        "    / /  ___    ___  | ' /  ___   ___  _ __    ___  _ __ \n" ,
        "   / /  / _ \\  / _ \\ |  <  / _ \\ / _ \\| '_ \\  / _ \\| '__|\n" ,
        "  / /__| (_) || (_) || . \\|  __/|  __/| |_) ||  __/| |   \n" ,
        " /_____|\\___/  \\___/ |_|\\_\\\\___| \\___|| .__/  \\___||_|   \n" ,
        "                                      | |                \n" ,
        "                                      |_|               ",
    };

    public static void printBanner() {
        for (String line : BANNER) {
            System.out.println(line);
        }
    }
}
