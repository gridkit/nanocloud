package org.gridkit.nanocloud.agent;

import java.util.concurrent.atomic.AtomicReference;

public class SampleAgent2 {
    public static final String NOT_SET = "NOT_SET";
    public static final AtomicReference<String> options = new AtomicReference<String>(NOT_SET);

    public static void premain(String args) {
        options.set(args);
    }

}
