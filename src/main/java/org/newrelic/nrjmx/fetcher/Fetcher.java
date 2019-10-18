package org.newrelic.nrjmx.fetcher;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Fetcher {
    CompletableFuture<Map<String, Object>> query(String beanName);
}
