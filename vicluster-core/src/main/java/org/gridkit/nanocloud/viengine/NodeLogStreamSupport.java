package org.gridkit.nanocloud.viengine;

import java.util.Map;

import org.gridkit.vicluster.ViEngine;
import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.LogStream;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.gridkit.zerormi.zlog.ZLogger;

class NodeLogStreamSupport implements LazyPragma, PragmaHandler {

    public static LogStream getStream(PragmaReader reader, String streamName) {
        
        Object factory = reader.get(Pragma.LOGGER_FACTORY + streamName);
        String loggerName = reader.get(Pragma.LOGGER_NAME + streamName);
        String level = reader.get(Pragma.LOGGER_LEVEL + streamName);
        
        ZLogger zlog = null;
        
        if (factory instanceof ZLogger) {
            zlog = (ZLogger) factory;
        }
        else if (factory == null) {
            zlog = ZLogFactory.getDefaultRootLogger();
        }
        else {
            throw new IllegalArgumentException("Invalid logger factory type: " + factory.getClass().getSimpleName());
        }
        
        if (loggerName != null) {
            if (loggerName.startsWith("~")) {
                loggerName = ViEngine.Core.transform(loggerName, streamName);
            }
        }
        else {
            loggerName = streamName;
        }
        
        LogLevel logLevel;
        if (level != null && level.trim().length() > 0) {
            logLevel = null;
            for(LogLevel l : LogLevel.values()) {
                if (level.trim().equalsIgnoreCase(l.name())) {
                    logLevel = l;
                    break;
                }
            }
            if (logLevel == null) {
                throw new IllegalArgumentException("Invalid log level '" + logLevel + "'");
            }
        }
        else {
            logLevel = LogLevel.DEBUG;
        }
        
        return zlog.get(loggerName, logLevel);        
    }
    
    @Override
    public Object resolve(String key, PragmaReader context) {
        if (key.startsWith(Pragma.LOGGER_STREAM)) {
            String streamName = key.substring(Pragma.LOGGER_STREAM.length());
            LogStream ls = getStream(context, streamName);
            return new LogStreamWrapper(streamName, ls);
        }
        else {
            throw new IllegalArgumentException("Key '" + key + "' is not starting with '" + Pragma.LOGGER_STREAM + "'");
        }
    }

    @Override
    public void init(PragmaWriter conext) {
        // do nothing        
    }

    @Override
    public Object query(PragmaWriter context, String key) {
        return context.get(key);
    }

    @Override
    public void apply(PragmaWriter context, Map<String, Object> values) {
        for(String key: values.keySet()) {
            context.set(key, values.get(key));
        }
        // refresh loggers
        for(String lskey: context.match(Pragma.LOGGER_STREAM + "**")) {
            Object o = context.get(lskey);
            if (o instanceof LogStreamWrapper) {
                ((LogStreamWrapper) o).update(context);
            }
        }
    }



    static class LogStreamWrapper implements LogStream {
        
        String loggerName;
        LogStream stream;

        public LogStreamWrapper(String loggerName, LogStream stream) {
            this.loggerName = loggerName;
            this.stream = stream;
        }

        public String getLoggerName() {
            return loggerName;
        }
        
        public void setStream(LogStream stream) {
            this.stream = stream;
        }
        
        public void update(PragmaReader context) {
            this.stream = getStream(context, loggerName);
        }
        
        public boolean isEnabled() {
            return stream.isEnabled();
        }

        public void log(String message) {
            stream.log(message);
        }

        public void log(Throwable e) {
            stream.log(e);
        }

        public void log(String format, Object... argument) {
            stream.log(format, argument);
        }
    }
}
