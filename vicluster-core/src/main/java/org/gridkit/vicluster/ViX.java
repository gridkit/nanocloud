package org.gridkit.vicluster;

import org.gridkit.vicluster.ViConf.Classpath;
import org.gridkit.vicluster.ViConf.Console;

public class ViX {

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
}
