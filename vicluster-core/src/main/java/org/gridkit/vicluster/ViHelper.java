/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.gridkit.nanocloud.Cloud;

public class ViHelper {
	
	/**
	 * Workaround for incremental migration from 0.7 to 0.8 branch 
	 * @deprecated
	 */
	public static void addStartupHook(ViConfigurable conf, String hookId, Runnable hook) {
	    conf.x(VX.HOOK).setStartupHook(hookId, hook);
	}

	/**
	 * Workaround for incremental migration from 0.7 to 0.8 branch 
	 * @deprecated
	 */
	public static void addShutdownHook(ViConfigurable conf, String hookId, Runnable hook) {
	    conf.x(VX.HOOK).setShutdownHook(hookId, hook);
	}
	
    public static void suspend(ViNode node) {
        throw new UnsupportedOperationException();        
    }

    public static void resume(ViNode node) {
        throw new UnsupportedOperationException();        
    }
	
	public static void configure(Cloud manager, String filename) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		if (is == null) {
			is = new FileInputStream(filename);
		}
		configure(manager, is);
	}

	@SuppressWarnings("serial")
	public static void configure(final Cloud manager, InputStream reader) throws IOException {
		new Properties() {
			@Override
			public  Object put(Object key, Object value) {
				String skey = (String) key;
				String svalue = (String) value;
				int pi = svalue.indexOf("=");
				pi = pi < 0 ? svalue.length() : pi;
				String pname = svalue.substring(0, pi).trim();
				String pvalue = svalue.substring(pi);
				if (pvalue.length() > 0) {
					pvalue = pvalue.substring(1);
				}
				manager.node(skey).setProp(pname, pvalue);
				return null;
			}
		}.load(reader);
		reader.close();
	}

	public static void configure(final Cloud manager, Iterable<Map.Entry<String, String>> props) {
		for(Map.Entry<String, String> prop : props) {
			String skey = prop.getKey();
			String svalue = prop.getValue();
			int pi = svalue.indexOf("=");
			pi = pi < 0 ? svalue.length() : pi;
			String pname = svalue.substring(0, pi).trim();
			String pvalue = svalue.substring(pi);
			if (pvalue.length() > 0) {
				pvalue = pvalue.substring(1);
			}
			manager.node(skey).setProp(pname, pvalue);
		}
	}
}
