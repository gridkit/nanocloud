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
package org.gridkit.vicluster.spi;

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
