package org.gridkit.nanocloud.telecontrol.ssh;

import static java.util.Collections.singleton;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_ADDRESS;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_JAR_CACHE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_JAVA_EXEC;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_JSCH_PREFERED_AUTH;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_PASSWORD;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.KEY_PRIVATE_KEY_FILE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_JAR_CACHE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_JSCH_OPTION;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_PASSWORD;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_PRIVATE_KEY_FILE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_TARGET_ACCOUNT;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_TARGET_HOST;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SSH_JSCH_OPTION;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.RemoteEx;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.WildProps;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;

public class SshConfigurer {

    private final Map<String, String> conf = new HashMap<String, String>();

    public void init(PropProvider props) {
        String url = props.get(RemoteEx.REMOTE_TARGET_URL);
        url = ViEngine.Core.transform(url, props.get(ViConf.NODE_NAME));

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e1) {
            throw new RuntimeException(e1);
        }

        if (uri.getPort() > 0) {
            conf.put(SPI_SSH_TARGET_HOST, uri.getHost() + ":" + uri.getPort());
        } else {
            conf.put(SPI_SSH_TARGET_HOST, uri.getHost());
        }

        if (uri.getUserInfo() != null) {
            conf.put(SPI_SSH_TARGET_ACCOUNT, uri.getUserInfo());
        }
        //

        if (props.get(ViConf.REMOTE_HOST) != null) {
            conf.put(SPI_SSH_TARGET_HOST, ViEngine.Core.transform(props.get(ViConf.REMOTE_HOST), props.get(ViConf.NODE_NAME)));
        }
        if (props.get(ViConf.REMOTE_ACCOUNT) != null) {
            conf.put(SPI_SSH_TARGET_ACCOUNT, props.get(ViConf.REMOTE_ACCOUNT));
        }

        String config = props.get(ViConf.REMOTE_HOST_CONFIG);
        if (config != null) {
            try {
                InputStream is = ViEngine.Core.openStream(config);
                if (is != null) {
                    WildProps wp = new WildProps();
                    wp.load(is);
                    processHostConfig(getOrFail(SPI_SSH_TARGET_HOST), conf.get(SPI_SSH_TARGET_ACCOUNT), wp, conf);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (props.get(RemoteNode.PASSWORD) != null) {
            conf.put(SPI_SSH_PASSWORD, props.get(RemoteNode.PASSWORD));
        }
        if (props.get(RemoteNode.SSH_KEY_FILE) != null) {
            conf.put(SPI_SSH_PRIVATE_KEY_FILE, props.get(RemoteNode.SSH_KEY_FILE));
        }
        if (props.get(SshSpiConf.SSH_PASSWORD) != null) {
            conf.put(SPI_SSH_PASSWORD, props.get(SshSpiConf.SSH_PASSWORD));
        }

        if (conf.get(SPI_SSH_TARGET_ACCOUNT) == null) {
            conf.put(SPI_SSH_TARGET_ACCOUNT, System.getProperty("user.name"));
        }
        if (conf.get(SPI_SSH_PASSWORD) == null && conf.get(SPI_SSH_PRIVATE_KEY_FILE) == null) {
            conf.put(SPI_SSH_PRIVATE_KEY_FILE, "~/.ssh/id_dsa|~/.ssh/id_rsa");
        }
        for (String key: props.keys(SSH_JSCH_OPTION)) {
            String opt = key.substring(SSH_JSCH_OPTION.length());
            conf.put(SPI_SSH_JSCH_OPTION + opt, props.get(key));
        }
        if (props.get(RemoteNode.SSH_AUTH_METHODS) != null) {
            conf.put(SPI_SSH_JSCH_OPTION + "PreferredAuthentications", props.get(RemoteNode.SSH_AUTH_METHODS));
        }

        if (!conf.containsKey(SPI_JAR_CACHE)) {
            conf.put(SPI_JAR_CACHE, resolveCachePath(props));
        }
        if (!conf.containsKey(SPI_BOOTSTRAP_JVM_EXEC)) {
            conf.put(SPI_BOOTSTRAP_JVM_EXEC, resolveBootCmd(props));
        }
    }

    protected String resolveBootCmd(PropProvider props) {
        String cmd = props.get(SshSpiConf.REMOTE_BOOTSTRAP_JVM_EXEC);
        if (cmd == null) {
            cmd = props.get(SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC);
        }
        if (cmd == null) {
            cmd = props.get(ViConf.JVM_EXEC_CMD);
        }
        if (cmd == null) {
            cmd = props.get(SshSpiConf.REMOTE_FALLBACK_JVM_EXEC);
        }
        if (cmd == null) {
            throw new RuntimeException("No Java executable configured");
        }
        return cmd;
    }

    protected String resolveCachePath(PropProvider props) {
        String path = props.get(SshSpiConf.SPI_JAR_CACHE);
        if (path == null) {
            path = props.get(SshSpiConf.REMOTE_JAR_CACHE);
        }
        if (path == null) {
            path = props.get(RemoteNode.JAR_CACHE_PATH);
        }
        if (path == null) {
            throw new RuntimeException("Jar cache path is not configured");
        }
        return path;
    }

    protected void processHostConfig(String host, String account, WildProps wp, Map<String, String> ec) {
        if (account == null) {
            account = wp.get(host);
            ec.put(SPI_SSH_TARGET_ACCOUNT, account);
        }
        if (account == null) {
            throw new RuntimeException("Cannot resolve remote account");
        }
        String key = account + "@" + host;
        ec.put(SPI_SSH_PASSWORD, wp.get(key + "!" + KEY_PASSWORD));
        ec.put(SPI_SSH_PRIVATE_KEY_FILE, wp.get(key + "!" + KEY_PRIVATE_KEY_FILE));
        if (wp.get(key + "!" + KEY_ADDRESS) != null) {
            ec.put(SPI_SSH_TARGET_HOST, wp.get(key + "!" + KEY_ADDRESS));
        }
        ec.put(SPI_BOOTSTRAP_JVM_EXEC, wp.get(key + "!" + KEY_JAVA_EXEC));
        ec.put(SPI_JAR_CACHE, wp.get(key + "!" + KEY_JAR_CACHE));
        ec.put(SPI_SSH_JSCH_OPTION + "PreferredAuthentications", wp.get(key + "!" + KEY_JSCH_PREFERED_AUTH));

        ec.values().removeAll(singleton(null));
    }

    public void configure(SimpleSshSessionProvider factory) {

        factory.setUser(conf.get(SPI_SSH_TARGET_ACCOUNT));

        for(String key: conf.keySet()) {
            if (key.startsWith(SPI_SSH_JSCH_OPTION)) {
                String opt = key.substring(SPI_SSH_JSCH_OPTION.length());
                factory.setConfig(opt, conf.get(key));
            }
        }

        String password = conf.get(SPI_SSH_PASSWORD);
        String keyFile = conf.get(SPI_SSH_PRIVATE_KEY_FILE);

        if (password != null) {
            factory.setPassword(password);
        }
        if (keyFile != null) {
            factory.setKeyFile(keyFile);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((conf == null) ? 0 : conf.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SshConfigurer other = (SshConfigurer) obj;
        if (conf == null) {
            if (other.conf != null)
                return false;
        } else if (!conf.equals(other.conf))
            return false;
        return true;
    }

    public interface PropProvider {

        String get(String prop);

        List<String> keys(String prefix);
    }

    public String getRemoteHost() {
        return getOrFail(SPI_SSH_TARGET_HOST);
    }

    public String getRemoteAccount() {
        return getOrFail(SPI_SSH_TARGET_ACCOUNT);
    }

    public String getBootCachePath() {
        return getOrFail(SPI_JAR_CACHE);
    }

    public String getBootCmd() {
        return getOrFail(SPI_BOOTSTRAP_JVM_EXEC);
    }

    private String getOrFail(String key) {
        String value = conf.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing param '" + key + "'");
        }
        return value;
    }
}
