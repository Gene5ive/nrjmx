package org.newrelic.nrjmx.err;

public class QueryException extends Exception {
    public QueryException(String message, Exception cause) {
        super(message, cause);
    }
}
