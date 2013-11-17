package org.gridkit.vicluster;

import org.gridkit.vicluster.ViConf.Console;

public class ViX {

	public static final ViExtender<Console> CONSOLE = new ViExtender<ViConf.Console>() {
		@Override
		public Console wrap(ViConfigurable node) {
			return Console.at(node);
		}
	};
}
