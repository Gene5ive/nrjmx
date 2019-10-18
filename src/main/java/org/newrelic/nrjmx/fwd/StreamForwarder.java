package org.newrelic.nrjmx.fwd;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

public class StreamForwarder implements Forwarder {

    private PrintStream output;

    public StreamForwarder() {
        this(System.out);
    }

    public StreamForwarder(OutputStream output) {
        this.output = new PrintStream(output);
    }

    @Override
    public void forward(Map<String, Object> items) {
        Gson gson = new Gson();
        output.println(gson.toJson(items));
    }

}
