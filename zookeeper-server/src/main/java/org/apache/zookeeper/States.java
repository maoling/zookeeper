package org.apache.zookeeper;

public enum States {
        CONNECTING,
        ASSOCIATING,
        CONNECTED,
        CONNECTEDREADONLY,
        CLOSED,
        AUTH_FAILED,
        NOT_CONNECTED;

        public boolean isAlive() {
            return this != CLOSED && this != AUTH_FAILED;
        }

        /**
         * Returns whether we are connected to a server (which
         * could possibly be read-only, if this client is allowed
         * to go to read-only mode)
         * */
        public boolean isConnected() {
            return this == CONNECTED || this == CONNECTEDREADONLY;
        }
    }