package org.gridkit.nanocloud.viengine;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConfExtender;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeExtender;
import org.gridkit.vicluster.VoidCallable;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ViEngine2Node implements ViNode {

	private ViEngine2 engine;
	private ViExecutor execProxy;
	
	public ViEngine2Node(ViEngine2 engine) {
		this.engine = engine;
		
		AdvancedExecutor exec = engine.getExecutor();
		execProxy = new ExecProxy(exec);
		
	}

	@Override
	public <X> X x(ViNodeExtender<X> extention) {
	    return extention.wrap(this);
	}

	@Override
	public <X> X x(ViConfExtender<X> extention) {
		return extention.wrap(this);
	}
	
	@Override
	public void touch() {
		// do nothing
	}
	
	public void exec(Runnable task) {
		execProxy.exec(task);
	}

	public void exec(VoidCallable task) {
		execProxy.exec(task);
	}

	public <T> T exec(Callable<T> task) {
		return execProxy.exec(task);
	}

	public Future<Void> submit(Runnable task) {
		return execProxy.submit(task);
	}

	public Future<Void> submit(VoidCallable task) {
		return execProxy.submit(task);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return execProxy.submit(task);
	}

	public <T> List<T> massExec(Callable<? extends T> task) {
		return execProxy.massExec(task);
	}

	public List<Future<Void>> massSubmit(Runnable task) {
		return execProxy.massSubmit(task);
	}

	public List<Future<Void>> massSubmit(VoidCallable task) {
		return execProxy.massSubmit(task);
	}

	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return execProxy.massSubmit(task);
	}

	@Override
	public void setProp(final String propName, final String value) {
		setProps(Collections.singletonMap(propName, value));
	}

	@Override
	public void setProps(Map<String, String> props) {
		for(String p: props.keySet()) {
			if (!ViConf.isVanilaProp(p)) {
				throw new IllegalArgumentException("[" + p + "] is not 'vanila' prop");
			}
		}
		Map<String, Object> pragmas = new HashMap<String, Object>();
		for(String key: props.keySet()) {
		    pragmas.put(Pragma.PROP + key, props.get(key));
		}
		
		engine.setPragmas(pragmas);
	}

	@Override
	public void setConfigElement(String key, Object value) {
		engine.setPragmas(Collections.singletonMap(key, value));
	}

	@Override
	public void setConfigElements(Map<String, Object> config) {
		engine.setPragmas(config);
	}

	@Override
	public String getProp(final String propName) {
		return (String)engine.getPragma(Pragma.PROP + propName);
	}

	@Override
	public Object getPragma(final String pragmaName) {
		return engine.getPragma(pragmaName);
	}

	@Override
	public void kill() {
		engine.kill();
	}

	@Override
	public void shutdown() {
		engine.shutdown();
	}

	private class ExecProxy extends AdvExecutor2ViExecutor {

		public ExecProxy(AdvancedExecutor advExec) {
			super(advExec);
		}

		@Override
		protected AdvancedExecutor getExecutor() {
			return super.getExecutor();
		}
	}
}
