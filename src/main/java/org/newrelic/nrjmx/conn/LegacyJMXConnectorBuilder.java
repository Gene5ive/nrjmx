package org.newrelic.nrjmx.conn;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// TODO: messy and unefficient. Left here for backwards-compatibility reasons. replace
public class LegacyJMXConnectorBuilder implements JMXConnectorFactory {
    private static final String DEFAULT_URI_PATH = "jmxrmi";
    private static final Boolean DEFAULT_J_BOSS_MODE_IS_STANDALONE = false;

    private Map<String, Object> connectionEnv = new HashMap<>();
    private final String hostname;
    private final int port;
    private String uriPath = DEFAULT_URI_PATH;
    private String username = "";
    private String password = "";
    private String keyStore = "";
    private String keyStorePassword = "";
    private String trustStore = "";
    private String trustStorePassword = "";
    private boolean remote;
    private boolean jbossStandalone = DEFAULT_J_BOSS_MODE_IS_STANDALONE;

    public LegacyJMXConnectorBuilder(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public LegacyJMXConnectorBuilder uriPath(String uriPath) {
        this.uriPath = uriPath;
        return this;
    }

    public LegacyJMXConnectorBuilder userPassword(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    public LegacyJMXConnectorBuilder keyStore(String keyStore, String keyStorePassword) {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public LegacyJMXConnectorBuilder trustStore(String trustStore, String trustStorePassword) {
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public LegacyJMXConnectorBuilder remote(boolean remote) {
        this.remote = remote;
        return this;
    }

    public LegacyJMXConnectorBuilder jbossStandalone(boolean jbossStandalone) {
        this.jbossStandalone = jbossStandalone;
        return this;
    }


    @Override
    public JMXConnector get() throws IOException {
        String connectionString;
        if (remote) {
            final String path;
            if (DEFAULT_URI_PATH.equals(uriPath)) { // this has no sense. Leaving here just to not break any backwards-compatible behavior
                path = "";
            } else {
                path = uriPath + "/";
            }

            String remoteProtocol = "remote";
            if (jbossStandalone) {
                remoteProtocol = "remote+http";
            }

            // Official doc for remoting v3 is not available, see:
            // - https://developer.jboss.org/thread/196619
            // - http://jbossremoting.jboss.org/documentation/v3.html
            // Some doc on URIS at:
            // - https://github.com/jboss-remoting/jboss-remoting/blob/master/src/main/java/org/jboss/remoting3/EndpointImpl.java#L292-L304
            // - https://stackoverflow.com/questions/42970921/what-is-http-remoting-protocol
            // - http://www.mastertheboss.com/jboss-server/jboss-monitoring/using-jconsole-to-monitor-a-remote-wildfly-server
            connectionString = String.format("service:jmx:%s://%s:%s%s", remoteProtocol, hostname, port, path);
        } else {
            connectionString = String.format("service:jmx:rmi:///jndi/rmi://%s:%s/%s", hostname, port, uriPath);
        }

        if (!"".equals(username)) {
            connectionEnv.put(JMXConnector.CREDENTIALS, new String[]{username, password});
        }

        if (!"".equals(keyStore) && !"".equals(trustStore)) {
            Properties p = System.getProperties();
            p.put("javax.net.ssl.keyStore", keyStore);
            p.put("javax.net.ssl.keyStorePassword", keyStorePassword);
            p.put("javax.net.ssl.trustStore", trustStore);
            p.put("javax.net.ssl.trustStorePassword", trustStorePassword);
            connectionEnv.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());
        }

        JMXServiceURL address = new JMXServiceURL(connectionString);
        return javax.management.remote.JMXConnectorFactory.connect(address, connectionEnv);
    }
}
