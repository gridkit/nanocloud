package org.gridkit.nanocloud.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.lab.interceptor.CutPoint;
import org.gridkit.lab.interceptor.Interception;
import org.gridkit.nanocloud.telecontrol.isolate.IsolateRemoteSessionWrapper;
import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.InductiveRule;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.isolate.Isolate;

public class InstrumentationInitializer implements Interceptor {

	public static final String INITIALIZER_NAME = "interceptor-support";
	public static InstrumentationInitializer INSTANCE = new InstrumentationInitializer();
	
	@Override
	public void process(String name, Phase phase, QuorumGame game) {
		if (phase == Phase.PRE_INIT) {
			ViEngine.Core.addRule(game, createInstrumentationWrapperRule());			
		}
		else if (phase == Phase.POST_INIT) {
			ViExecutor vex = new AdvExecutor2ViExecutor(game.getManagedProcess().getExecutionService());
			final List<String> shared = new ArrayList<String>();
			shared.add(IsolateInstrumentationSupport.class.getName());
			shared.add(IsolateInstrumentationSupport.class.getName());
			shared.add(IsolateInstrumentationSupport.class.getName());
			shared.add(CutPoint.class.getName());
			shared.add(org.gridkit.lab.interceptor.Interceptor.class.getName());
			shared.add(Interception.class.getName());
			
			vex.exec(new Runnable() {
				@Override
				public void run() {
					Isolate isolate = Isolate.currentIsolate();
					if (isolate == null) {
						throw new RuntimeException("Isolate is not enabled, cannot activate instrumentation");
					}
					for(String c: shared) {
						isolate.addClassRule(c, false);
					}
				}
			});
		}
		
	}

	private InductiveRule createInstrumentationWrapperRule() {
		return new WrapperActivatorRule();
	}
	
	private static class WrapperActivatorRule implements InductiveRule {

		@Override
		public boolean apply(QuorumGame game) {
			if (game.getNodeType() == ViConf.NODE_TYPE__ISOLATE) {
				if (game.getInstrumentationWrapper() != null) {
					game.unsetProp(ViConf.SPI_INSTRUMENTATION_WRAPPER);
					return true;
				}
			}
			else {
				if (game.getInstrumentationWrapper() == null) {
					game.setProp(ViConf.SPI_INSTRUMENTATION_WRAPPER, new IsolateRemoteSessionWrapper());
					game.unsetProp(ViConf.SPI_INSTRUMENTATION_WRAPPER_APPLIED);
					return true;
				}
			}
			
			return false;
		}
		
	}

	@Override
	public void processAddHoc(String name, ViExecutor node) {
		// TODO verify instrumentation support
	}	
}
