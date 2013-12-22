package org.gridkit.vicluster;

import org.gridkit.vicluster.ViConf.Classpath;
import org.gridkit.vicluster.ViConf.CommonConfig;
import org.gridkit.vicluster.ViConf.Console;
import org.gridkit.vicluster.ViConf.ProcessConfig;

public class ViX {

	public static final ViExtender<CommonConfig> COMMON = new ViExtender<ViConf.CommonConfig>() {
		@Override
		public CommonConfig wrap(ViConfigurable node) {
			return CommonConfig.at(node);
		}
	};
	
	
	/**
	 * @see Console
	 */
	public static final ViExtender<Console> CONSOLE = new ViExtender<ViConf.Console>() {
		@Override
		public Console wrap(ViConfigurable node) {
			return Console.at(node);
		}
	};

	/**
	 * @see Classpath
	 */
	public static final ViExtender<Classpath> CLASSPATH = new ViExtender<ViConf.Classpath>() {
		@Override
		public Classpath wrap(ViConfigurable node) {
			return Classpath.at(node);
		}
	};

	/**
	 * @see ProcessConfig
	 */
	public static final ViExtender<ProcessConfig> PROCESS = new ViExtender<ProcessConfig>() {
		@Override
		public ProcessConfig wrap(ViConfigurable node) {
			return ProcessConfig.at(node);
		}
	};
}
