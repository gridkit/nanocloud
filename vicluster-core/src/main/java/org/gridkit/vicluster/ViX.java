package org.gridkit.vicluster;

import org.gridkit.nanocloud.telecontrol.isolate.IsolateConfig;
import org.gridkit.vicluster.ViConf.Classpath;
import org.gridkit.vicluster.ViConf.CommonConfig;
import org.gridkit.vicluster.ViConf.Console;
import org.gridkit.vicluster.ViConf.ProcessConfig;
import org.gridkit.vicluster.ViConfigurable.Delegate;

public class ViX {

	public static final ViConfExtender<CommonConfig> COMMON = new ViConfExtender<ViConf.CommonConfig>() {
		@Override
		public CommonConfig wrap(ViConfigurable node) {
			return CommonConfig.at(node);
		}
	};
	
	/**
	 * @see Console
	 */
	public static final ViConfExtender<Console> CONSOLE = new ViConfExtender<ViConf.Console>() {
		@Override
		public Console wrap(ViConfigurable node) {
			return Console.at(node);
		}
	};

	/**
	 * @see Classpath
	 */
	public static final ViConfExtender<Classpath> CLASSPATH = new ViConfExtender<ViConf.Classpath>() {
		@Override
		public Classpath wrap(ViConfigurable node) {
			return Classpath.at(node);
		}
	};

	/**
	 * @see ProcessConfig
	 */
	public static final ViConfExtender<ProcessConfig> PROCESS = new ViConfExtender<ProcessConfig>() {
		@Override
		public ProcessConfig wrap(ViConfigurable node) {
			return ProcessConfig.at(node);
		}
	};
	
	public static final ViConfExtender<IsolateConfig> ISOLATE = new ViConfExtender<IsolateConfig>() {
		
		@Override
		public IsolateConfig wrap(ViConfigurable node) {
			return IsolateConfig.at(node);
		}
	};
	
	public static class HookConfig extends Delegate {

		private ViConfigurable conf;
		
		public static HookConfig at(ViConfigurable conf) {
			return new HookConfig(conf);
		}
		
		public HookConfig(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}
		
		public HookConfig addStartupHook(Runnable hook) {
			throw new UnsupportedOperationException();
		}

		public HookConfig setStartupHook(String id, Runnable hook) {
			throw new UnsupportedOperationException();			
		}

		public HookConfig addShutdownHook(Runnable hook) {
			throw new UnsupportedOperationException();
		}
		
		public HookConfig setShutdownHook(String id, Runnable hook) {
			throw new UnsupportedOperationException();			
		}
		
		public HookConfig addPostShutdownHook(Runnable hook) {
			throw new UnsupportedOperationException();
		}
		
		public HookConfig setPostShutdownHook(String id, Runnable hook) {
			throw new UnsupportedOperationException();			
		}
	}
}
