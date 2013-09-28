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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class WildProps implements Serializable {
	
	private static final long serialVersionUID = 20121018L;
	
	private List<Entry> entries = new ArrayList<WildProps.Entry>();
	private Map<String, Entry> index = new HashMap<String, WildProps.Entry>();
	
	public void put(String key, String value) {
		Entry entry = new Entry(key, value);
		entries.add(entry);
		if (!isPattern(key)) {
			index.put(key, entry);
		}
	}

	public void putAll(Map<String, String> values) {
		for(Map.Entry<String, String> e: values.entrySet()) {
			put(e.getKey(), e.getValue());
		}
	}
	
	public String get(String key) {
		if (index.containsKey(key)) {
			return index.get(key).value;
		}
		else {
			Entry e = lookup(key);
			return e == null ? null : e.value;
		}
	}
	
	private Entry lookup(String key) {
		Entry r = null;
		for(Entry entry: entries) {
			if (entry.matchKey(key)) {
				r = entry;
				// continue to find last
			}
		}
		return r;
	}

	public void load(InputStream is) throws IOException {
		new PropLoader().load(is);
		is.close();
	}

	public void load(Reader reader) throws IOException {
		new PropLoader().load(reader);
		reader.close();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Map.Entry<String, String>> entryList() {
		return Collections.unmodifiableList(((List)entries));
	}

	private static boolean isPattern(String key) {
		return key.indexOf('*') >= 0 || key.indexOf('?') >= 0;
	}
	
	@SuppressWarnings("serial")
	private final class PropLoader extends Properties {
		
		@Override
		public  Object put(Object key, Object value) {
			String skey = (String) key;
			String svalue = (String) value;
			WildProps.this.put(skey, svalue);
			return null;
		}
	}

	private static class Entry implements Map.Entry<String, String> {
		
		String key;
		Pattern pattern;
		String value;
		
		Entry(String key, String value) {
			this.key = key;
			this.value = value;
			if (isPattern(key)) {
				pattern = GlobHelper.translate(key, ".");
			}
		}
		
		public boolean matchKey(String key) {
			if (pattern != null) {
				return match(key) || match("." + key) || match(key + ".") || match("." + key + ".");
			}
			else {
				return this.key.equals(key);
			}
		}
		
		private boolean match(String key) {			
			return pattern.matcher(key).matches();
		}

		@Override
		public String getKey() {
			return key;
		}
		
		@Override
		public String getValue() {
			return value;
		}
		
		@Override
		public String setValue(String value) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public String toString() {
			return key + ": " + value;
		}
	}
}
