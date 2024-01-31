package org.gridkit.nanocloud.telecontrol.ssh;

import static org.gridkit.nanocloud.VX.REMOTE;

import org.gridkit.nanocloud.RemoteEx;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViConfigurable;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.nanocloud.viengine.PragmaHandler;
import org.gridkit.vicluster.ViConf;

public class SshConf extends RemoteEx<SshConf> {

    public static final ViConfExtender<SshConf> SSH = new ViConfExtender<SshConf>() {

        @Override
        public SshConf wrap(ViConfigurable node) {
            SshConf ssh = new SshConf(node);
            node.x(REMOTE);
            node.setConfigElement(Pragma.REMOTE_PROTOCOL + "ssh", SshConnectorAction.INSTANCE);
            node.setConfigElement(Pragma.NODE_PRAGMA_HANDLER + "ssh", PragmaHandler.PASSIVE);
            node.setProp(Pragma.DEFAULT + Pragma.REMOTE_SCHEME_DEFAULT, "ssh");

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

    @Deprecated
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
}
