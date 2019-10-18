package org.newrelic.nrjmx.conn;

import javax.management.remote.JMXConnector;
import java.io.IOException;

public interface JMXConnectorFactory {
    JMXConnector get() throws IOException;
}
