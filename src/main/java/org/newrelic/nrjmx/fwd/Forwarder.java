package org.newrelic.nrjmx.fwd;

import java.util.Map;

public interface Forwarder {
    void forward(Map<String, Object> items);
}
