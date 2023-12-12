package org.gridkit.vicluster;

import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.viengine.BootAnnotation;
import org.gridkit.nanocloud.viengine.NodeAction;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.nanocloud.viengine.PragmaWriter;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViEngine.QuorumGame;

public class Hooks {

    public static class StratupHook implements Interceptor, NodeAction {

        private final Runnable runnable;

        public StratupHook(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            // Engine2 support
            AdvancedExecutor executor = context.get(Pragma.RUNTIME_EXECUTOR);
            try {
                executor.submit(runnable).get();
            } catch (Error e) {
            	throw e;
            } catch (Throwable e) {
            	if (e instanceof ExecutionException) {
            		e = e.getCause();
            	}
                BootAnnotation.fatal(context, e, "Failed to process startup hook - " + e.toString());
            }
        }

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            if (phase == Phase.POST_INIT) {
                game.addUniqueProp(ViConf.ACTIVATED_REMOTE_HOOK + name, runnable);
            }
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            node.exec(runnable);
        }

    }

    public static class ShutdownHook implements Interceptor, NodeAction {

        private final Runnable runnable;

        public ShutdownHook(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            // Engine2 support
            AdvancedExecutor executor = context.get(Pragma.RUNTIME_EXECUTOR);
            try {
                executor.submit(runnable).get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            // ignore
        }

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            if (phase == Phase.PRE_SHUTDOWN) {
                game.addUniqueProp(ViConf.ACTIVATED_REMOTE_HOOK + name, runnable);
            }
        }
    }

    public static class ShutdownHostHook implements Interceptor, NodeAction {

        private final Runnable runnable;

        public ShutdownHostHook(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            // Engine2 support
            runnable.run();
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            // ignore
        }

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            if (phase == Phase.PRE_SHUTDOWN) {
                game.addUniqueProp(ViConf.ACTIVATED_HOST_HOOK + name, runnable);
            }
        }
    }

    public static class PostShutdownHook implements Interceptor, NodeAction {

        private final Runnable runnable;

        public PostShutdownHook(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            // Engine2 support
            runnable.run();
        }

        @Override
        public void processAdHoc(String name, ViExecutor node) {
            // ignore
        }

        @Override
        public void process(String name, Phase phase, QuorumGame game) {
            if (phase == Phase.POST_SHUTDOWN) {
                game.addUniqueProp(ViConf.ACTIVATED_HOST_HOOK + name, runnable);
            }
        }
    }
}
