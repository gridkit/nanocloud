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
package org.gridkit.vicluster.telecontrol.bootstraper;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Dummy main class to ensure that JVM starts normally.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class HalloWorld {
	public static void main(String[] args) throws IOException {
		System.out.println("\"Hallo world\" from " + InetAddress.getLocalHost().getHostName());
		
//		for(URL url: ((URLClassLoader)Thread.currentThread().getContextClassLoader()).getURLs()) {
//			System.out.println(url);
//		}
//		
//		Enumeration en = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
//		while(en.hasMoreElements()) {
//			URL x = (URL) en.nextElement();
//			System.out.println(x.toString());
//			InputStream is = x.openStream();
//			while(true) {
//				int c = is.read();
//				if (c >= 0) {
//					System.out.print((char)c);
//				}
//				else {
//					break;
//				}
//			}
//			is.close();
//			System.out.println();
//		}
//
//		System.out.println();
//
//		en = Thread.currentThread().getContextClassLoader().getResources(Bootstraper.class.getName().replace('.', '/') + ".class");
//		while(en.hasMoreElements()) {
//			URL x = (URL) en.nextElement();
//			System.out.println(x.toString());
//		}
	}	
}
