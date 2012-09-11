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
package org.gridkit.vicluster.isolate;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class IsolateProps {

	public static String NAME = "isolate:name";
	/** Use for packages to be isolate */
	public static String PACKAGE = "isolate:package:";
	/** Use for classes to be delegated to parent classloader */
	public static String SHARED = "isolate:shared:";
	/** Use for adding additional URLs to classpath */
	public static String CP_ADD = "isolate:cp-add:";
	/** Use for prohibiting URLs in classpath */
	public static String CP_REMOVE = "isolate:cp-remove:";

}
