package org.newrelic.nrjmx.err;

public class ConnectionException extends Exception {
    public ConnectionException(String message, Exception cause) {
        super(message, cause);
    }
}

