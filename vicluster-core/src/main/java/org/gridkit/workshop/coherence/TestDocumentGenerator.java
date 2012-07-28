package org.gridkit.workshop.coherence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("serial")
public class TestDocumentGenerator implements Serializable {

	Map<String, FieldDescription> fields = new LinkedHashMap<String, TestDocumentGenerator.FieldDescription>();
	int documentCount;
	
	public int getDocCount() {
		return documentCount;
	}
	
	public void setDocCount(int docs) {
		documentCount = docs;
	}
	
	public List<String> getFieldList() {
		return new ArrayList<String>(fields.keySet());
	}
	
	public void addField(String field, double selectivity) {
		fields.put(field, new FieldDescription(field, selectivity, false));
	}

	public void addField(String field, double selectivity, boolean relative) {
		fields.put(field, new FieldDescription(field, selectivity, relative));
	}
	
	public double getSelectivity(String field) {
		double selectivity = fields.get(field).selectivity;
		if (fields.get(field).relativeSelectivity) {
			selectivity *= documentCount;
		}
		return selectivity;
	}
	
	public Map<String, String> getDoc(int id) {
		Random rnd = new Random(id);
		Map<String, String> map = new HashMap<String, String>();
		map.put("ID", String.valueOf(id));
		for(FieldDescription fd: fields.values()) {
			String fieldName = fd.fieldName;
			int range = (int)(documentCount / getSelectivity(fieldName));
			if (range < 1) {
				range = 1;
			}
			int sn = rnd.nextInt(range);
			map.put(fieldName, getTerm(fieldName, sn));
		}
		return map;
	}
	
	public String getRandomTerm(Random rnd, String field) {
		int range = (int)(documentCount / getSelectivity(field));
		return getTerm(field, rnd.nextInt(range));
	}

	public String[] getRandomRange(Random rnd, String field, int len) {
		int range = (int)(documentCount / getSelectivity(field));
		String[] r = new String[2];
		int low = rnd.nextInt(range - len);
		r[0] = getTerm(field, low);
		r[1] = getTerm(field, low + len);
		
		return r;
	}
	
	public String getTerm(String field, int sn) {
		Random rnd = new Random(field.hashCode() ^ sn);
		int len = rnd.nextInt(16) + 1;
		char[] buf = new char[len];
		for(int i = 0; i != len; ++i) {
			buf[i] = (char) ('A' + rnd.nextInt(26));
		}
		int n = 0x40000000 | sn;
		return Integer.toHexString(n) + "-" + new String(buf);
	}
	
	private static class FieldDescription implements Serializable {
		
		String fieldName;
		double selectivity;		
		boolean relativeSelectivity;
		
		public FieldDescription(String fieldName, double selectivity, boolean relativeSelectivity) {
			this.fieldName = fieldName;
			this.selectivity = selectivity;
			this.relativeSelectivity = relativeSelectivity;
		}
	}
}
