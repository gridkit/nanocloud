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

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ViProps {

	/**
	 * Type of ViNode (e.g. isolate, local ...). Types have associated provider.
	 */
	public static final String NODE_TYPE = "node:type";
	
	public static final String NODE_TYPE_ISOLATE = "isolate";
	public static final String NODE_TYPE_LOCAL = "local";
	public static final String NODE_TYPE_REMOTE = "remote";
	public static final String NODE_TYPE_ALIAS = "alias";
	
	/**
	 * Name of node.
	 */
	public static final String NODE_NAME = "node:name";

	/**
	 * Arbitrary UID of ViNode. May be used by certain providers.
	 */	
	public static final String NODE_UID = "node:uid";

	/**
	 * Logical "host" of which node is meant to run (might be another vinode name for example).
	 */	
	public static final String HOST = "node:host";

	/**
	 * Label
	 */	
	public static final String LABEL = "label:";	
}
