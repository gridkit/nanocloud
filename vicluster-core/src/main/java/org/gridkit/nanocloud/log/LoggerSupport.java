package org.gridkit.nanocloud.log;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViEngine.PragmaHandler;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViEngine.WritableSpiConfig;
import org.gridkit.zerormi.DirectRemoteExecutor;
import org.gridkit.zerormi.zlog.LogLevel;
import org.gridkit.zerormi.zlog.ZLogger;

public class LoggerSupport implements Interceptor {

    @Override
    public void process(String name, Phase phase, QuorumGame game) {
        if (phase == Phase.PRE_INIT) {
            initLogger(game);
        }
    }

    @Override
    public void processAdHoc(String name, DirectRemoteExecutor node) {
        throw new IllegalStateException("Node is initialized");
    }


    private void initLogger(QuorumGame game) {
        String lp = game.get(ViConf.LOG_LOGGER_PROVIDER);
        if ("slf4j".equals(lp)) {
            lp = Slf4jInstantiator.class.getName();
        }
        if (lp == null) {
            throw new NullPointerException(ViConf.LOG_LOGGER_PROVIDER + " is null");
        }
        LoggerInstantiator li;
        try {
            li = (LoggerInstantiator) Class.forName(lp).newInstance();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> cfg = new LinkedHashMap<String, Object>(game.getConfigProps(ViConf.LOG_LOGGER));
        String root = game.get(ViConf.LOG_LOGGER_ROOT);
        if (root != null) {
            root = ViEngine.Core.transform(root, game.getNodeName());
            cfg.put(ViConf.LOG_LOGGER_ROOT, root);
        }
        ZLogger logger = li.instantiate(cfg);
        ZLogFilter zlf = new ZLogFilter(logger, LogLevel.INFO);
        game.setProp(ViConf.SPI_LOGGER, zlf);

        LogConfigHandler phandler = new LogConfigHandler();
        game.setProp(ViConf.PRAGMA_HANDLER__LOG, phandler);
        for(String key: game.getConfigProps(ViConf.LOG).keySet()) {
            phandler.applyLogConfig(key, game.getProp(key), zlf);
        }
    }

    private static class LogConfigHandler implements PragmaHandler {

        @Override
        public Object get(String key, ViEngine engine) {
            return engine.getConfig().get(key);
        }

        @Override
        public void set(String key, Object value, ViEngine engine, WritableSpiConfig writableConfig) {
            if (key.startsWith(ViConf.LOG_LOGGER)) {
                if (!engine.isStarted()) {
                    throw new IllegalStateException("Logger is already initialized (" + key + ")");
                }
            }
            else if (key.startsWith(ViConf.LOG)) {
                ConfigurableZLogger zlog = engine.getConfig().get(ViConf.SPI_LOGGER);
                applyLogConfig(key, value, zlog);
            }
            else {
                throw new IllegalArgumentException("Unknown config key '" + value + "'");
            }
        }

        void applyLogConfig(String key, Object value, ConfigurableZLogger zlog) {
            String path = key.substring(ViConf.LOG.length());
            if (path.indexOf(':') >= 0) {
                throw new IllegalArgumentException("Unknown config key '" + value + "'");
            }
            String lvl = (String) value;
            LogLevel l;
            if (lvl == null) {
                l = null;
            }
            else {
                l = LogLevel.valueOf(lvl.toUpperCase());
            }
            zlog.setLevel(path, l);
        }
    }
}
