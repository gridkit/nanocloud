package org.gridkit.vicluster.spi;

import java.net.MalformedURLException;
import java.net.URL;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.isolate.Isolate;

public class IsolateNodeInstantiator implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		Isolate isolate = (Isolate)config.getLast(IsolateInstantiator.ISOLATE);
		
		ViNodeSpi nodeSpi = new SimpleIsolateViNodeSpi(isolate);
		NodeSpiHelper.initViNodeSPI(nodeSpi, context, config);
		
		return nodeSpi;
	}
	
	static class SimpleIsolateViNodeSpi extends AbstractViNodeSpi implements IsolateViNodeSpi {
		
		private final Isolate isolate;

		public SimpleIsolateViNodeSpi(Isolate isolate) {
			this.isolate = isolate;
		}

		@Override
		public void includePackage(String packageName) {
			isolate.addPackage(packageName);
		}

		@Override
		public void excludeClass(String className) {
			isolate.exclude(className);
		}

		@Override
		public void excludeClass(Class<?> type) {
			isolate.exclude(type);
		}

		@Override
		public void addToClasspath(String url) {
			try {
				isolate.addToClasspath(new URL(url));
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}

		@Override
		public void addToClasspath(URL url) {
			isolate.addToClasspath(url);
		}

		@Override
		public void removeFromClasspath(String url) {
			try {
				isolate.removeFromClasspath(new URL(url));
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		@Override
		public void removeFromClasspath(URL url) {
			isolate.removeFromClasspath(url);
		}

		@Override
		public AdvancedExecutor getExecutor() {
			return isolate;
		}

		@Override
		protected void destroy() {
			isolate.stop();			
		}
	}	
}
