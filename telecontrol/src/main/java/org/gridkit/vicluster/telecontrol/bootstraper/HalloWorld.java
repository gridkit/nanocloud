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
	
	public static int EXIT_CODE = 10;
	
	private static void displayProp(String prefix, String name) {
		System.out.println(prefix + " " + name + " = " + System.getProperty(name));
	}
	
	public static void main(String[] args) throws IOException {
		String user = System.getProperty("user.name");
		String localhost = InetAddress.getLocalHost().getHostName(); 
		
		displayProp(user + "@" + localhost, "user.name");
		displayProp(user + "@" + localhost, "user.home");
		displayProp(user + "@" + localhost, "user.dir");
		displayProp(user + "@" + localhost, "java.home");
		displayProp(user + "@" + localhost, "java.runtime.name");
		displayProp(user + "@" + localhost, "java.runtime.version");
		displayProp(user + "@" + localhost, "java.vm.name");
		
		System.exit(EXIT_CODE);
	}	
}
