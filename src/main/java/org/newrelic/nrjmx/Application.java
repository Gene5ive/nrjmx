package org.newrelic.nrjmx;

import org.apache.commons.cli.HelpFormatter;
import org.newrelic.nrjmx.conn.JMXConnectorFactory;
import org.newrelic.nrjmx.conn.LegacyJMXConnectorBuilder;
import org.newrelic.nrjmx.fetcher.Fetcher;
import org.newrelic.nrjmx.fetcher.LegacyFetcher;
import org.newrelic.nrjmx.fwd.Forwarder;
import org.newrelic.nrjmx.fwd.StreamForwarder;

import javax.management.remote.JMXConnector;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {
    public static void printHelp() {
        new HelpFormatter().printHelp("nrjmx", Arguments.options());
    }

    public static void main(String[] args) {
        Arguments cliArgs = null;
        try {
            cliArgs = Arguments.from(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            printHelp();
            System.exit(1);
        }

        if (cliArgs.isHelp()) {
            printHelp();
            System.exit(0);
        }

        Logger logger = Logger.getLogger("nrjmx");
        Logging.setup(logger, cliArgs.isVerbose());
        Executor exec = Executors.newCachedThreadPool();

        JMXConnectorFactory cf = new LegacyJMXConnectorBuilder(cliArgs.getHostname(), cliArgs.getPort())
                .uriPath(cliArgs.getUriPath())
                .userPassword(cliArgs.getUsername(), cliArgs.getPassword())
                .keyStore(cliArgs.getKeyStore(), cliArgs.getKeyStorePassword())
                .trustStore(cliArgs.getTrustStore(), cliArgs.getTrustStorePassword())
                .remote(cliArgs.getIsRemoteJMX())
                .jbossStandalone(cliArgs.getIsRemoteJBossStandalone());
        Forwarder output = new StreamForwarder();

        try (JMXConnector conn = cf.get()) {
            Fetcher fetcher = new LegacyFetcher(conn.getMBeanServerConnection(), exec);
            new StdinAcceptor(fetcher, System.in, output).run().get(); // wait for all the tasks to complete
        } catch (Exception e) {
            logger.severe(e.getMessage());
            logger.log(Level.FINE, e.getMessage(), e);
            System.exit(1);
        }

        logger.fine("wait for all the missing tasks to complete");
        logger.fine("tasks complete");
    }
}
