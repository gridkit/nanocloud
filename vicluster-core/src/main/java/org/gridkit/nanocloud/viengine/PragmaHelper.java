package org.gridkit.nanocloud.viengine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gridkit.zerormi.zlog.LogStream;

class PragmaHelper {

    public static final String LOG_STREAM__NO_PRAGMA_HANDLER = "node.config.pragma_handle_not_found";
    public static final String LOG_STREAM__HOOK_EXECUTION_EXCEPTON = "node.runtime.hook_execution_exception";


    public static String transform(String pattern, String name) {
        if (pattern == null || !pattern.startsWith("~")) {
            return pattern;
        }
        int n = pattern.indexOf('!');
        if (n < 0) {
            throw new IllegalArgumentException("Invalid host extractor [" + pattern + "]");
        }
        String format = pattern.substring(1, n);
        Matcher m = Pattern.compile(pattern.substring(n + 1)).matcher(name);
        if (!m.matches()) {
            throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
        }
        else {
            Object[] groups = new Object[m.groupCount()];
            for(int i = 0; i != groups.length; ++i) {
                groups[i] = m.group(i + 1);
                try {
                    groups[i] = Long.parseLong((String)groups[i]);
                }
                catch(NumberFormatException e) {
                    // ignore
                }
            }
            try {
                return String.format(format, groups);
            }
            catch(IllegalArgumentException e) {
                throw new IllegalArgumentException("Host extractor [" + pattern + "] is not applicable to name '" + name + "'");
            }
        }
    }

    public static Object getPragma(PragmaWriter context, String key) {
        String pragmaType = pragmaTypeOf(key);
        PragmaHandler handler = context.get(Pragma.NODE_PRAGMA_HANDLER + pragmaType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler for '" + key + "'");
        }
        return handler.query(context, key);
    }

    public static void setPragmas(PragmaWriter context, boolean initial, Map<String, Object> pragmas) {
        Map<String, Object> buffer = new LinkedHashMap<String, Object>();
        String lastPrag = null;
        for(String key: pragmas.keySet()) {
            String pragmaType = pragmaTypeOf(key);
            if (lastPrag != null && !lastPrag.equals(pragmaType)) {
                PragmaHandler handler = context.get(Pragma.NODE_PRAGMA_HANDLER + lastPrag);
                if (handler == null) {
                    log(context, LOG_STREAM__NO_PRAGMA_HANDLER, "No handler for pargma '" + lastPrag + "'");
                }
                if (initial) {
                    handler.setup(context, buffer);
                } else {
                    handler.apply(context, buffer);
                }
                buffer.clear();
            }
            lastPrag = pragmaType;
            buffer.put(key, pragmas.get(key));
        }
        if (lastPrag != null) {
            PragmaHandler handler = context.get(Pragma.NODE_PRAGMA_HANDLER + lastPrag);
            if (handler == null) {
                log(context, LOG_STREAM__NO_PRAGMA_HANDLER, "No handler for pargma '" + lastPrag + "'");
            }
            if (initial) {
                handler.setup(context, buffer);
            } else {
                handler.apply(context, buffer);
            }
        }
    }

    public static void runHooks(PragmaWriter context, String hookKey) {
        for(String key: context.match(hookKey + "**")) {
            try {
                NodeAction action = context.get(key);
                if (action != null) {
                    action.run(context);
                }
            }
            catch(Exception e) {
                log(context, LOG_STREAM__HOOK_EXECUTION_EXCEPTON, "Exception at '" + key + "' - %s", e);
            }
        }
    }

    public static void log(PragmaReader reader, String streamName, String message, Object... args) {
        ((LogStream)reader.get(Pragma.LOGGER_STREAM + streamName)).log(message, args);
    }

    private static String pragmaTypeOf(String key) {
        int c = key.indexOf(':');
        if (c < 0) {
            throw new IllegalArgumentException("Invalid key format '" + key + "' - no pragma");
        }
        return key.substring(0, c);
    }
}
