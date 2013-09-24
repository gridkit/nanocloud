package org.gridkit.vicluster;

import org.gridkit.vicluster.ViNodeLifeCycleHelper.Interceptor;
import org.gridkit.vicluster.ViNodeLifeCycleHelper.Phase;
import org.gridkit.vicluster.ViNodeLifeCycleHelper.QuorumGame;

class Hooks {

	public static class StratupHook implements Interceptor {

		private final Runnable runnable;
		
		public StratupHook(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void process(String name, Phase phase, QuorumGame game) {
			if (phase == Phase.POST_INIT) {
				game.addUniqueProp(ViNodeLifeCycleHelper.ACTIVATED_REMOTE_HOOK + name, runnable);
			}
		}
		
		@Override
		public void processAddHoc(String name, ViNode node) {
			node.exec(runnable);
		}
		
	}

	public static class ShutdownHook implements Interceptor {
		
		private final Runnable runnable;
		
		public ShutdownHook(Runnable runnable) {
			this.runnable = runnable;
		}
		
		@Override
		public void processAddHoc(String name, ViNode node) {
			// ignore
		}

		@Override
		public void process(String name, Phase phase, QuorumGame game) {
			if (phase == Phase.PRE_SHUTDOWN) {
				game.addUniqueProp(ViNodeLifeCycleHelper.ACTIVATED_REMOTE_HOOK + name, runnable);
			}
		}
	}
}
