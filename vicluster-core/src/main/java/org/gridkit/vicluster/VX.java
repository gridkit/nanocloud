package org.gridkit.vicluster;

import org.gridkit.nanocloud.telecontrol.isolate.IsolateConfig;
import org.gridkit.vicluster.ViConf.ClasspathConf;
import org.gridkit.vicluster.ViConf.TypeConf;
import org.gridkit.vicluster.ViConf.ConsoleConf;
import org.gridkit.vicluster.ViConf.HookConf;
import org.gridkit.vicluster.ViConf.JvmConf;

public class VX {

	public static final ViConfExtender<TypeConf> TYPE = new ViConfExtender<ViConf.TypeConf>() {
		@Override
		public TypeConf wrap(ViConfigurable node) {
			return TypeConf.at(node);
		}
	};
	
	/**
	 * @see ConsoleConf
	 */
	public static final ViConfExtender<ConsoleConf> CONSOLE = new ViConfExtender<ViConf.ConsoleConf>() {
		@Override
		public ConsoleConf wrap(ViConfigurable node) {
			return ConsoleConf.at(node);
		}
	};

	/**
	 * @see ClasspathConf
	 */
	public static final ViConfExtender<ClasspathConf> CLASSPATH = new ViConfExtender<ViConf.ClasspathConf>() {
		@Override
		public ClasspathConf wrap(ViConfigurable node) {
			return ClasspathConf.at(node);
		}
	};

	/**
	 * @see JvmConf
	 */
	public static final ViConfExtender<JvmConf> PROCESS = new ViConfExtender<JvmConf>() {
		@Override
		public JvmConf wrap(ViConfigurable node) {
			return JvmConf.at(node);
		}
	};
	
	public static final ViConfExtender<IsolateConfig> ISOLATE = new ViConfExtender<IsolateConfig>() {
		
		@Override
		public IsolateConfig wrap(ViConfigurable node) {
			return IsolateConfig.at(node);
		}
	};

	public static final ViConfExtender<HookConf> HOOK = new ViConfExtender<HookConf>() {
		
		@Override
		public HookConf wrap(ViConfigurable node) {
			return HookConf.at(node);
		}
	};
}
