package org.gridkit.lab.interceptor;

import java.util.ArrayList;
import java.util.List;

public class ByteCodeHelper {

	public static String[] parseParamTypeNames(String signature) {
		List<String> result = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		int c = signature.lastIndexOf(')');
		String types = signature.substring(1, c);
		boolean longName = false;
		for(int i = 0; i != types.length(); ++i) {
			char x  = types.charAt(i);
			if ('[' == x) {
				sb.append(x);
			}
			else if (';' == x) {
				sb.append(x);
				result.add(sb.toString());
				sb.setLength(0);
				longName = false;
			}
			else if ('L' == x) {
				sb.append(x);
				longName = true;
			}
			else if (longName){
				sb.append(x);
			}
			else {
				sb.append(x);
				result.add(sb.toString());
				sb.setLength(0);
			}
		}
		return result.toArray(new String[result.size()]);
	}
}
