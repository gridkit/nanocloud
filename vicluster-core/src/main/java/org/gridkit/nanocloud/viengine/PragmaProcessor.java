package org.gridkit.nanocloud.viengine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PragmaProcessor implements NodeAction {

    public static PragmaProcessor CONFIGURE = new PragmaProcessor(true);
    public static PragmaProcessor INIT = new PragmaProcessor(false);

    private final boolean configure;

    public PragmaProcessor(boolean configure) {
        this.configure = configure;
    }

    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        if (configure) {
            for (PragmaHandler ph: context.collect(Pragma.NODE_PRAGMA_HANDLER + "**", PragmaHandler.class)) {
                ph.configure(context);
            }
        } else {
            Map<String, PragmaHandler> handlers = new LinkedHashMap<String, PragmaHandler>();
            for(String key: context.match("**")) {
                if (key.startsWith("#")) {
                    continue;
                }
                String pragType = pragmaTypeOf(key);
                if (!handlers.containsKey(pragType)) {
                    PragmaHandler ph = context.get(Pragma.NODE_PRAGMA_HANDLER + pragType);
                    if (ph == null) {
                        throw new IllegalArgumentException("No handler for pragma '" + key + "'");
                    }
                    else {
                        handlers.put(pragType, ph);
                    }
                }
            }
            for(PragmaHandler h: handlers.values()) {
                h.init(context);
            }
            Map<String, Object> confSnap = new LinkedHashMap<String, Object>();
            for(String key: context.match("**")) {
                if (key.startsWith("#")) {
                    continue;
                }
                confSnap.put(key, context.get(key));
            }
            PragmaHelper.setPragmas(context, true, confSnap);
        }
    }

    private static String pragmaTypeOf(String key) {
        int c = key.indexOf(':');
        if (c < 0) {
            throw new IllegalArgumentException("Invalid key format '" + key + "' - no pragma");
        }
        return key.substring(0, c);
    }
}
