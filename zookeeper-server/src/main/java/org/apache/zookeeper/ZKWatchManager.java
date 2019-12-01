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

package org.apache.zookeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.zookeeper.server.watch.PathParentIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage watchers and handle events generated by the ClientCnxn object.
 *
 * This class is intended to be packaged-private so that it doesn't serve
 * as part of ZooKeeper client API.
 */
class ZKWatchManager implements ClientWatchManager {

    private static final Logger LOG = LoggerFactory.getLogger(ZKWatchManager.class);

    private final Map<String, Set<Watcher>> dataWatches = new HashMap<>();
    private final Map<String, Set<Watcher>> existWatches = new HashMap<>();
    private final Map<String, Set<Watcher>> childWatches = new HashMap<>();
    private final Map<String, Set<Watcher>> persistentWatches = new HashMap<>();
    private final Map<String, Set<Watcher>> persistentRecursiveWatches = new HashMap<>();
    private final boolean disableAutoWatchReset;

    private volatile Watcher defaultWatcher;

    ZKWatchManager(boolean disableAutoWatchReset, Watcher defaultWatcher) {
        this.disableAutoWatchReset = disableAutoWatchReset;
        this.defaultWatcher = defaultWatcher;
    }

    void setDefaultWatcher(Watcher watcher) {
        this.defaultWatcher = watcher;
    }

    Watcher getDefaultWatcher() {
        return this.defaultWatcher;
    }

    List<String> getDataWatchList() {
        synchronized (dataWatches) {
            return new ArrayList<>(dataWatches.keySet());
        }
    }

    List<String> getChildWatchList() {
        synchronized (childWatches) {
            return new ArrayList<>(childWatches.keySet());
        }
    }

    List<String> getExistWatchList() {
        synchronized (existWatches) {
            return new ArrayList<>(existWatches.keySet());
        }
    }

    List<String> getPersistentWatchList() {
        synchronized (persistentWatches) {
            return new ArrayList<>(persistentWatches.keySet());
        }
    }

    List<String> getPersistentRecursiveWatchList() {
        synchronized (persistentRecursiveWatches) {
            return new ArrayList<>(persistentRecursiveWatches.keySet());
        }
    }

    Map<String, Set<Watcher>> getDataWatches() {
        return dataWatches;
    }

    Map<String, Set<Watcher>> getExistWatches() {
        return existWatches;
    }

    Map<String, Set<Watcher>> getChildWatches() {
        return childWatches;
    }

    Map<String, Set<Watcher>> getPersistentWatches() {
        return persistentWatches;
    }

    Map<String, Set<Watcher>> getPersistentRecursiveWatches() {
        return persistentRecursiveWatches;
    }

    private void addTo(Set<Watcher> from, Set<Watcher> to) {
        if (from != null) {
            to.addAll(from);
        }
    }

    public Map<Watcher.Event.EventType, Set<Watcher>> removeWatcher(
        String clientPath,
        Watcher watcher,
        Watcher.WatcherType watcherType,
        boolean local,
        int rc
    ) throws KeeperException {
        // Validate the provided znode path contains the given watcher of watcherType
        containsWatcher(clientPath, watcher, watcherType);

        Map<Watcher.Event.EventType, Set<Watcher>> removedWatchers = new HashMap<>();
        HashSet<Watcher> childWatchersToRem = new HashSet<>();
        removedWatchers.put(Watcher.Event.EventType.ChildWatchRemoved, childWatchersToRem);
        HashSet<Watcher> dataWatchersToRem = new HashSet<>();
        removedWatchers.put(Watcher.Event.EventType.DataWatchRemoved, dataWatchersToRem);
        HashSet<Watcher> persistentWatchersToRem = new HashSet<>();
        removedWatchers.put(Watcher.Event.EventType.PersistentWatchRemoved, persistentWatchersToRem);
        boolean removedWatcher = false;
        switch (watcherType) {
        case Children: {
            synchronized (childWatches) {
                removedWatcher = removeWatches(childWatches, watcher, clientPath, local, rc, childWatchersToRem);
            }
            break;
        }
        case Data: {
            synchronized (dataWatches) {
                removedWatcher = removeWatches(dataWatches, watcher, clientPath, local, rc, dataWatchersToRem);
            }

            synchronized (existWatches) {
                boolean removedDataWatcher = removeWatches(existWatches, watcher, clientPath, local, rc, dataWatchersToRem);
                removedWatcher |= removedDataWatcher;
            }
            break;
        }
        case Any: {
            synchronized (childWatches) {
                removedWatcher = removeWatches(childWatches, watcher, clientPath, local, rc, childWatchersToRem);
            }

            synchronized (dataWatches) {
                boolean removedDataWatcher = removeWatches(dataWatches, watcher, clientPath, local, rc, dataWatchersToRem);
                removedWatcher |= removedDataWatcher;
            }

            synchronized (existWatches) {
                boolean removedDataWatcher = removeWatches(existWatches, watcher, clientPath, local, rc, dataWatchersToRem);
                removedWatcher |= removedDataWatcher;
            }

            synchronized (persistentWatches) {
                boolean removedPersistentWatcher = removeWatches(persistentWatches,
                        watcher, clientPath, local, rc, persistentWatchersToRem);
                removedWatcher |= removedPersistentWatcher;
            }

            synchronized (persistentRecursiveWatches) {
                boolean removedPersistentRecursiveWatcher = removeWatches(persistentRecursiveWatches,
                        watcher, clientPath, local, rc, persistentWatchersToRem);
                removedWatcher |= removedPersistentRecursiveWatcher;
            }
        }
        }
        // Watcher function doesn't exists for the specified params
        if (!removedWatcher) {
            throw new KeeperException.NoWatcherException(clientPath);
        }
        return removedWatchers;
    }

    private boolean contains(String path, Watcher watcherObj, Map<String, Set<Watcher>> pathVsWatchers) {
        boolean watcherExists = true;
        if (pathVsWatchers == null || pathVsWatchers.size() == 0) {
            watcherExists = false;
        } else {
            Set<Watcher> watchers = pathVsWatchers.get(path);
            if (watchers == null) {
                watcherExists = false;
            } else if (watcherObj == null) {
                watcherExists = watchers.size() > 0;
            } else {
                watcherExists = watchers.contains(watcherObj);
            }
        }
        return watcherExists;
    }

    /**
     * Validate the provided znode path contains the given watcher and
     * watcherType
     *
     * @param path
     *            - client path
     * @param watcher
     *            - watcher object reference
     * @param watcherType
     *            - type of the watcher
     * @throws KeeperException.NoWatcherException
     */
    void containsWatcher(String path, Watcher watcher, Watcher.WatcherType watcherType) throws KeeperException.NoWatcherException {
        boolean containsWatcher = false;
        switch (watcherType) {
        case Children: {
            synchronized (childWatches) {
                containsWatcher = contains(path, watcher, childWatches);
            }

            synchronized (persistentWatches) {
                boolean contains_temp = contains(path, watcher,
                        persistentWatches);
                containsWatcher |= contains_temp;
            }

            synchronized (persistentRecursiveWatches) {
                boolean contains_temp = contains(path, watcher,
                        persistentRecursiveWatches);
                containsWatcher |= contains_temp;
            }
            break;
        }
        case Data: {
            synchronized (dataWatches) {
                containsWatcher = contains(path, watcher, dataWatches);
            }

            synchronized (existWatches) {
                boolean contains_temp = contains(path, watcher, existWatches);
                containsWatcher |= contains_temp;
            }

            synchronized (persistentWatches) {
                boolean contains_temp = contains(path, watcher,
                        persistentWatches);
                containsWatcher |= contains_temp;
            }

            synchronized (persistentRecursiveWatches) {
                boolean contains_temp = contains(path, watcher,
                        persistentRecursiveWatches);
                containsWatcher |= contains_temp;
            }
            break;
        }
        case Any: {
            synchronized (childWatches) {
                containsWatcher = contains(path, watcher, childWatches);
            }

            synchronized (dataWatches) {
                boolean contains_temp = contains(path, watcher, dataWatches);
                containsWatcher |= contains_temp;
            }

            synchronized (existWatches) {
                boolean contains_temp = contains(path, watcher, existWatches);
                containsWatcher |= contains_temp;
            }

            synchronized (persistentWatches) {
                boolean contains_temp = contains(path, watcher,
                        persistentWatches);
                containsWatcher |= contains_temp;
            }

            synchronized (persistentRecursiveWatches) {
                boolean contains_temp = contains(path, watcher,
                        persistentRecursiveWatches);
                containsWatcher |= contains_temp;
            }
        }
        }
        // Watcher function doesn't exists for the specified params
        if (!containsWatcher) {
            throw new KeeperException.NoWatcherException(path);
        }
    }

    protected boolean removeWatches(
        Map<String, Set<Watcher>> pathVsWatcher,
        Watcher watcher,
        String path,
        boolean local,
        int rc,
        Set<Watcher> removedWatchers
    ) throws KeeperException {
        if (!local && rc != KeeperException.Code.OK.intValue()) {
            throw KeeperException.create(KeeperException.Code.get(rc), path);
        }
        boolean success = false;
        // When local flag is true, remove watchers for the given path
        // irrespective of rc. Otherwise shouldn't remove watchers locally
        // when sees failure from server.
        if (rc == KeeperException.Code.OK.intValue() || (local && rc != KeeperException.Code.OK.intValue())) {
            // Remove all the watchers for the given path
            if (watcher == null) {
                Set<Watcher> pathWatchers = pathVsWatcher.remove(path);
                if (pathWatchers != null) {
                    // found path watchers
                    removedWatchers.addAll(pathWatchers);
                    success = true;
                }
            } else {
                Set<Watcher> watchers = pathVsWatcher.get(path);
                if (watchers != null) {
                    if (watchers.remove(watcher)) {
                        // found path watcher
                        removedWatchers.add(watcher);
                        // cleanup <path vs watchlist>
                        if (watchers.size() <= 0) {
                            pathVsWatcher.remove(path);
                        }
                        success = true;
                    }
                }
            }
        }
        return success;
    }

    @Override
    public Set<Watcher> materialize(
        Watcher.Event.KeeperState state,
        Watcher.Event.EventType type,
        String clientPath
    ) {
        Set<Watcher> result = new HashSet<>();

        switch (type) {
        case None:
            result.add(defaultWatcher);
            boolean clear = disableAutoWatchReset && state != Watcher.Event.KeeperState.SyncConnected;
            synchronized (dataWatches) {
                for (Set<Watcher> ws : dataWatches.values()) {
                    result.addAll(ws);
                }
                if (clear) {
                    dataWatches.clear();
                }
            }

            synchronized (existWatches) {
                for (Set<Watcher> ws : existWatches.values()) {
                    result.addAll(ws);
                }
                if (clear) {
                    existWatches.clear();
                }
            }

            synchronized (childWatches) {
                for (Set<Watcher> ws : childWatches.values()) {
                    result.addAll(ws);
                }
                if (clear) {
                    childWatches.clear();
                }
            }

            synchronized (persistentWatches) {
                for (Set<Watcher> ws: persistentWatches.values()) {
                    result.addAll(ws);
                }
            }

            synchronized (persistentRecursiveWatches) {
                for (Set<Watcher> ws: persistentRecursiveWatches.values()) {
                    result.addAll(ws);
                }
            }

            return result;
        case NodeDataChanged:
        case NodeCreated:
            synchronized (dataWatches) {
                addTo(dataWatches.remove(clientPath), result);
            }
            synchronized (existWatches) {
                addTo(existWatches.remove(clientPath), result);
            }
            addPersistentWatches(clientPath, result);
            break;
        case NodeChildrenChanged:
            synchronized (childWatches) {
                addTo(childWatches.remove(clientPath), result);
            }
            addPersistentWatches(clientPath, result);
            break;
        case NodeDeleted:
            synchronized (dataWatches) {
                addTo(dataWatches.remove(clientPath), result);
            }
            // TODO This shouldn't be needed, but just in case
            synchronized (existWatches) {
                Set<Watcher> list = existWatches.remove(clientPath);
                if (list != null) {
                    addTo(list, result);
                    LOG.warn("We are triggering an exists watch for delete! Shouldn't happen!");
                }
            }
            synchronized (childWatches) {
                addTo(childWatches.remove(clientPath), result);
            }
            addPersistentWatches(clientPath, result);
            break;
        default:
            String errorMsg = String.format(
                "Unhandled watch event type %s with state %s on path %s",
                type,
                state,
                clientPath);
            LOG.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        return result;
    }

    private void addPersistentWatches(String clientPath, Set<Watcher> result) {
        synchronized (persistentWatches) {
            addTo(persistentWatches.get(clientPath), result);
        }
        synchronized (persistentRecursiveWatches) {
            for (String path : PathParentIterator.forAll(clientPath).asIterable()) {
                addTo(persistentRecursiveWatches.get(path), result);
            }
        }
    }
}
