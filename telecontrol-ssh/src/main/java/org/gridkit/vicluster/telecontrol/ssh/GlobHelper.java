package org.gridkit.vicluster.telecontrol.ssh;

import java.util.regex.Pattern;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class GlobHelper {
	
	/**
	 * GLOB pattern supports *, ** and ? wild cards.
	 */
	public static Pattern translate(String pattern, String separator) {
		StringBuffer sb = new StringBuffer();
		String es = escape(separator);
		for(int i = 0; i != pattern.length(); ++i) {
			char c = pattern.charAt(i);
			if (c == '?') {
				sb.append("[^" + es + "]");
			}
			else if (c == '*') {
				if (i + 1 < pattern.length() && pattern.charAt(i+1) == '*') {
					i++;
					// **
					sb.append(".*");
				}
				else {
					sb.append("[^" + es + "]*");
				}
			}
			else {
				if (Character.isJavaIdentifierPart(c) || Character.isWhitespace(c)) {
					sb.append(c);
				}
				else {
					sb.append('\\').append(c);
				}
			}
		}
		return Pattern.compile(sb.toString());
	}

	private static String escape(String separator) {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i != separator.length(); ++i) {
			char c = separator.charAt(i);
			if ("\\[]&-".indexOf(c) >= 0){
				sb.append('\\').append(c);
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
