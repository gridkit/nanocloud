package org.gridkit.vicluster;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

public class ViHelper {
	
	public static void configure(ViManager manager, String filename) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		if (is == null) {
			is = new FileInputStream(filename);
		}
		configure(manager, is);
	}

	@SuppressWarnings("serial")
	public static void configure(final ViManager manager, InputStream reader) throws IOException {
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

	public static void configure(final ViManager manager, Iterable<Map.Entry<String, String>> props) {
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
