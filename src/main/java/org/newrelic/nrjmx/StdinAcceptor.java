package org.newrelic.nrjmx;

import org.newrelic.nrjmx.fetcher.Fetcher;
import org.newrelic.nrjmx.fwd.Forwarder;

import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StdinAcceptor {

    private static final Logger LOG = Logger.getLogger(StdinAcceptor.class.getName());

    private final InputStream inputStream;
    private final Fetcher fetcher;
    private final Forwarder output;

    public StdinAcceptor(Fetcher fetcher, InputStream inputStrean, Forwarder output) {
        this.inputStream = inputStrean;
        this.fetcher = fetcher;
        this.output = output;
    }

    public CompletableFuture run() {
        ConcurrentLinkedQueue<CompletableFuture> missingTasks = new ConcurrentLinkedQueue<>();

        Scanner input = new Scanner(inputStream);
        while (input.hasNextLine()) {
            String beanName = input.nextLine();
            missingTasks.add(fetcher.query(beanName)
                    .handle((stringObjectMap, ex) -> {
                        if (ex != null) {
                            LOG.severe(ex.getMessage());
                            LOG.log(Level.FINE, ex.getMessage(), ex);
                            return null;
                        }
                        output.forward(stringObjectMap);
                        return null;
                    }));
        }
        return CompletableFuture.allOf(missingTasks.toArray(new CompletableFuture[0]));
    }
}
