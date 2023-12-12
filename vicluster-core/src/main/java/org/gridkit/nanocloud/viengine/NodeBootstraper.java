package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gridkit.nanocloud.NodeConfigurationException;

class NodeBootstraper {

    private PragmaWriter nodeConfig;
//    private List<String> executionLog = new ArrayList<String>();

    public NodeBootstraper(PragmaMap nodeConfig) {
        this.nodeConfig = nodeConfig;
    }

    protected void freeze(String key) {
        // TODO
    }

    public void boot() {
        String nodeName = nodeConfig.get(Pragma.NODE_NAME);
        if (nodeName == null) {
            throw new NodeConfigurationException("ViNode name is missing");
        }
        NodeAction initializer;
        try {
            initializer = nodeConfig.get(Pragma.BOOT_TYPE_INITIALIZER);
            if (initializer != null) {
                initializer.run(nodeConfig);
            }
        }
        catch(Exception e) {
            throw new NodeConfigurationException("Failed to execute node initializer", e);
        }
        if (initializer == null) {
            BootAnnotation.warning("-", "Missing type initializer").append(nodeConfig);
        }

        freeze(Pragma.BOOT_SEQUENCE);
        for(String phase: getBootsequnce()) {
            processSubphase(phase);
        }
        nodeConfig.set(Pragma.BOOT_PHASE, "");
        finalCheck();
    }

    private void processSubphase(String subphase) {
        checkErrors();
        nodeConfig.set(Pragma.BOOT_PHASE, subphase);

        for(String subsub: getSubphases(subphase, Pragma.BOOT_PHASE_PRE)) {
            processSubphase(subsub);
        }

        processActions(subphase);

        for(String subsub: getSubphases(subphase, Pragma.BOOT_PHASE_POST)) {
            processSubphase(subsub);
        }

        checkErrors();
        processValidators(subphase);
        checkErrors();
    }

    private void processValidators(String subphase) {
        for(String vkey: nodeConfig.match(Pragma.BOOT_VALIDATOR + subphase + ".**")) {
            try {
                NodeAction check = nodeConfig.get(vkey);
                check.run(nodeConfig);
            }
            catch(Exception e) {
                BootAnnotation.fatal(subphase, "Exception processing '" + vkey + "' - " + e.toString())
                .append(nodeConfig);
                checkErrors();
            }
        }
    }

    private void processActions(String subphase) {
        for(String akey: nodeConfig.match(Pragma.BOOT_ACTION + subphase + ".**")) {
            try {
                NodeAction action = nodeConfig.get(akey);
                action.run(nodeConfig);
            }
            catch(Exception e) {
                BootAnnotation.fatal(subphase, e, "Exception processing '" + akey + "' - " + e.toString())
                    .append(nodeConfig);
                checkErrors();
            }
        }
    }

    private void finalCheck() {
        checkErrors();
        for(String vkey: nodeConfig.match(Pragma.BOOT_VALIDATOR + "**")) {
            try {
                NodeAction check = nodeConfig.get(vkey);
                check.run(nodeConfig);
            }
            catch(Exception e) {
                BootAnnotation.fatal("", "Exception processing '" + vkey + "' - " + e.toString())
                .append(nodeConfig);
                checkErrors();
            }
        }

        checkErrors();
    }

    private void checkErrors() {
        List<String> keys = nodeConfig.match(BootAnnotation.ERROR_PATTERN);
        Throwable error = null;
        if (!keys.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            String nodeName = nodeConfig.get(Pragma.NODE_NAME);
            sb.append("Node [" + nodeName + "] intialization failed at phase " + nodeConfig.get(Pragma.BOOT_PHASE));
            for(String mkey: nodeConfig.match(BootAnnotation.ANNOTATION_PATTERN)) {
                Object msg = nodeConfig.get(mkey);
                sb.append("\n  ").append(msg);
                if (msg instanceof BootAnnotation) {
                    Throwable e = ((BootAnnotation) msg).getExecption();
                    if (e != null) {
                        error = e;
                    }
                }
            }
            if (error != null) {
                throw new NodeConfigurationException(sb.toString(), error);
            } else {
                throw new NodeConfigurationException(sb.toString());
            }
        }
    }

    private List<String> getSubphases(String phase, String subphaseKey) {
        String pref = subphaseKey + phase + ".";
        String match = subphaseKey + phase + ".**";
        freeze(match);
        List<String> phases = new ArrayList<String>();
        List<String> keys = nodeConfig.match(match);
        for(String key: keys) {
            if (key.length() <= pref.length()) {
                BootAnnotation.fatal(phase, "Invalid subphase key \"%s\"", key).append(nodeConfig);
            }
            else {
                String subname = key.substring(pref.length());
                String subphase = phase + "-" + subname;
                if (isValidPhaseName(subname)) {
                    phases.add(subphase);
                }
                else {
                    BootAnnotation.fatal(phase, "Invalid subphase key \"%s\"", key).append(nodeConfig);
                }
            }

        }
        checkErrors();
        return phases;
    }

    private List<String> getBootsequnce() {
        String bootseq = nodeConfig.get(Pragma.BOOT_SEQUENCE);
        if (bootseq == null) {
            throw new NodeConfigurationException("No boot sequence defined " + Pragma.BOOT_SEQUENCE + ": " + nodeConfig.describe(bootseq));
        }
        String[] bs = bootseq.split("\\s+");
        if (bs.length == 0) {
            throw new NodeConfigurationException("No boot sequence [" + bootseq + "] is empty");
        }
        Set<String> set = new HashSet<String>();
        for(String p: bs) {
            if (set.contains(p)) {
                throw new NodeConfigurationException("No boot sequence [" + bootseq + "] is invalid, duplicated phase");
            }
            if (!isValidPhaseName(p)) {
                throw new NodeConfigurationException("No boot sequence [" + bootseq + "] is invalid, bad phase name '" + p + "'");
            }
            set.add(p);
        }
        return new ArrayList<String>(Arrays.asList(bs));
    }

    private boolean isValidPhaseName(String name) {
        if ("final-check".equals("name")) {
            return false;
        }
        for(int i = 0; i != name.length(); ++i) {
            char c = name.charAt(i);
            if (!(Character.isJavaIdentifierPart(c))) {
                return false;
            }
        }
        return true;
    }

//    private static class ActionLogEntry {
//
//        String phase;
//        String actionKey;
//        Object actionObject;
//        Map<String, String> reads = new LinkedHashMap<String, String>();
//        Map<String, String> writes = new LinkedHashMap<String, String>();
//
//    }
}
