package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.viengine.NodeConfigHelper.cloudSingleton;

import org.gridkit.nanocloud.telecontrol.LocalControlConsole;

class LocalNodeTypeInitializer extends SlaveJvmNodeTypeInitializer {

    @Override
    protected void configureHostControlConsoleSubphase(PragmaWriter config) {
        super.configureHostControlConsoleSubphase(config);
        cloudSingleton(config, Pragma.RUNTIME_HOST_CONTROL_CONSOLE, LocalControlConsole.class, "terminate");
    }
}
