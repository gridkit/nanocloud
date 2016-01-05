package org.gridkit.zerormi.zlog;

import org.junit.Test;

public class ZLogToSlf4JCheck {

    @Test
    public void check_logging() {
        ZLogFactory.getSlf4JRootLogger().get("test", LogLevel.WARN).log("Test message");
        ZLogFactory.getStdErrRootLogger().get("test", LogLevel.WARN).log("Test message");
    }    
}
