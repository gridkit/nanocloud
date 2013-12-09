package org.gridkit.nanocloud.telecontrol.isolate;

import java.util.Collections;

import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.PragmaHandler;
import org.gridkit.vicluster.ViEngine.WritableSpiConfig;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.gridkit.vicluster.isolate.IsolateSelfInitializer;

public class IsolatePragmaSupport implements PragmaHandler {

	@Override
	public Object get(String key, ViEngine engine) {
		return null;
	}

	@Override
	public void set(String key, Object value, ViEngine engine, WritableSpiConfig writableConfig) {
		if (	key.startsWith(IsolateProps.ISOLATE_PACKAGE)
			 ||	key.startsWith(IsolateProps.SHARE_PACKAGE)
			 ||	key.startsWith(IsolateProps.ISOLATE_CLASS)
			 ||	key.startsWith(IsolateProps.SHARE_CLASS)) {
			
			String val = (String)value;
			ViExecutor exec = new AdvExecutor2ViExecutor(engine.getConfig().getManagedProcess().getExecutionService());
			exec.exec(new IsolateSelfInitializer(Collections.singletonMap(key, val)));
		}
		else {
			throw new IllegalArgumentException("Unsupported pragma: " + key);
		}
	}
}
