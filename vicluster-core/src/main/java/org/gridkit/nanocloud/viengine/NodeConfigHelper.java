package org.gridkit.nanocloud.viengine;

import java.util.concurrent.ExecutionException;

import org.gridkit.vicluster.CloudContext.Helper;

public class NodeConfigHelper {
    
    private static PassivePragmaHandler PASSIVE_PRAGMA = new PassivePragmaHandler();
    private static PassthroughPragmaHandler PASSTHROUGH_PRAGMA = new PassthroughPragmaHandler();

    public static void require(PragmaWriter writer, String phase, String key) {
        require(writer, phase, key, null);
    }

    public static void require(PragmaWriter writer, String phase, String key, String description) {
        String vkey = Pragma.BOOT_VALIDATOR + phase + ".require:" + key;
        writer.set(vkey, new PresenceCheck(key, description));
    }

    public static void addPrePhase(PragmaWriter writer, String phase, String subphase) {
        writer.set(Pragma.BOOT_PHASE_PRE + phase + "." + subphase, "");
    }

    public static void addPostPhase(PragmaWriter writer, String phase, String subphase) {
        writer.set(Pragma.BOOT_PHASE_POST + phase + "." + subphase, "");
    }

    public static void action(PragmaWriter writer, String phase, String actionKey, NodeAction action) {
        writer.set(Pragma.BOOT_ACTION + phase + "." + actionKey, action);
    }    

    public static void actionLink(PragmaWriter writer, String phase, String actionKey, String linkTarget) {
        writer.link(Pragma.BOOT_ACTION + phase + "." + actionKey, linkTarget);
    }    
    
    public static void passivePragma(PragmaWriter writer, String pragma) {
        pragmaHandler(writer, pragma, PASSIVE_PRAGMA);
    }    

    public static void passthroughPragma(PragmaWriter writer, String pragma) {
        pragmaHandler(writer, pragma, PASSTHROUGH_PRAGMA);
    }    
    
    public static void pragmaHandler(PragmaWriter writer, String pragma, PragmaHandler handler) {
        writer.set(Pragma.NODE_PRAGMA_HANDLER + pragma, handler);        
    }
    
    public static void setDefault(PragmaWriter writer, String key, Object value) {
        writer.set(Pragma.DEFAULT + key, value);
    }

    public static void setLazyDefault(PragmaWriter writer, String key, LazyPragma lazy) {
        writer.setLazy(Pragma.DEFAULT + key, lazy);
    }
    
    public static <T> void cloudSingleton(PragmaWriter writer, String key, Class<T> type, String shutdownMethod) {
        SharedEntity<T> entry = new SharedEntity<T>(Helper.key(type), Helper.reflectionProvider(type, shutdownMethod));
        writer.setLazy(key, entry);
    }

    static class PresenceCheck implements NodeAction {
        
        private String key;
        private String description;

        public PresenceCheck(String key, String description) {
            this.key = key;
            this.description = description;
        }

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            if (context.get(key) == null) {
                BootAnnotation.fatal((String)context.get(Pragma.BOOT_PHASE), "Missing required key '" + key + "'" + (description == null ? "" : " - " + description))
                    .append(context);
            }            
        }

        @Override
        public String toString() {
            return "PresenceCheck[" + key + "]";
        }
    }
}
