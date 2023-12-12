package org.gridkit.nanocloud.telecontrol.ssh;

import static org.gridkit.nanocloud.VX.REMOTE;

import org.gridkit.nanocloud.RemoteEx;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConfExtender;
import org.gridkit.vicluster.ViConfigurable;

public class SshConf extends RemoteEx {

    public static final ViConfExtender<SshConf> SSH = new ViConfExtender<SshConf>() {

        @Override
        public SshConf wrap(ViConfigurable node) {
            SshConf ssh = new SshConf(node);
            node.x(REMOTE);
            node.setConfigElement(Pragma.REMOTE_PROTOCOL + "ssh", SshConnectorAction.INSTANCE);

            return ssh;
        }
    };

    SshConf(ViConfigurable node) {
        super(node);
    }

    public SshConf configureSimpleRemoting() {
        setSshConfigFile("?~/ssh-credentials.prop");
        setRemoteUrl("~ssh://%s!(.*)");

        return this;
    }

    public SshConf setRemoteUrl(String url) {
        setConfigElement(RemoteEx.REMOTE_TARGET_URL, url);
        return this;
    }

    public SshConf setRemoteHost(String host) {
        setConfigElement(ViConf.REMOTE_HOST, host);
        return this;
    }

    public SshConf setSshConfigFile(String configPath) {
        setConfigElement(ViConf.REMOTE_HOST_CONFIG, configPath);
        return this;
    }

    public SshConf setRemoteAccount(String account) {
        setConfigElement(ViConf.REMOTE_ACCOUNT, account);
        return this;
    }

    public SshConf setPassword(String password) {
        setConfigElement(RemoteNode.PASSWORD, password);
        return this;
    }

    public SshConf setPrivateKeyFile(String privateKeyFile) {
        setConfigElement(RemoteNode.SSH_KEY_FILE, privateKeyFile);
        return this;
    }

    public SshConf setJSchOptions(String jschKey, String jschValue) {
        setConfigElement(SshSpiConf.SSH_JSCH_OPTION + jschKey, jschValue);
        return this;
    }

    public SshConf setPreferedAuth(String auth) {
        setConfigElement(RemoteNode.SSH_AUTH_METHODS, auth);
        return this;
    }

    @Override
    public SshConf setRemoteNodeType() {
        super.setRemoteNodeType();
        return this;
    }

    @Override
    public SshConf setRemoteJavaExec(String javaCmd) {
        super.setRemoteJavaExec(javaCmd);
        return this;
    }

    @Override
    public SshConf setRemoteJarCachePath(String jarCachePath) {
        super.setRemoteJarCachePath(jarCachePath);
        return this;
    }

    @Override
    public SshConf setTargetUrl(String url) {
        super.setTargetUrl(url);
        return this;
    }
}
