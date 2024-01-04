package org.gridkit.nanocloud.viengine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.AgentEntry;

class AgentConfigurator implements NodeAction {

    public static final AgentConfigurator INSTANCE = new AgentConfigurator();

    @Override
    public void run(PragmaWriter context) throws ExecutionException {
        context.set(Pragma.RUNTIME_AGENTS, buildAgents(context));
    }

    protected List<AgentEntry> buildAgents(PragmaWriter context) {
        final List<String> keys = context.match(ViConf.JVM_AGENT + "**");
        List<AgentEntry> agentEntries = new ArrayList<AgentEntry>();
        for (String key : keys) {
            String agentAndOptions = context.get(key);
            final int delimiterIndex = agentAndOptions.indexOf("=");
            final File file = new File(agentAndOptions.substring(0, delimiterIndex));
            if (!file.exists()) {
                throw new IllegalArgumentException("Can not find agent file " + file);
            }
            final String options = agentAndOptions.substring(delimiterIndex + 1);
            final AgentEntry agentEntry = new AgentEntry(file, options.isEmpty() ? null : options);
            agentEntries.add(agentEntry);
        }
        return agentEntries;
    }
}
