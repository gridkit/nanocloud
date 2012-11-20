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
	public static final String NODE_TYPE_IN_PROCESS = "in-process";
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
	
	public static ViProps at(ViConfigurable config) {
		return new ViProps(config);
	}
	
	private final ViConfigurable config;

	private ViProps(ViConfigurable config) {
		this.config = config;
	}

	public ViProps setType(String type) {
		config.setProp(NODE_TYPE, type);
		return this;
	}

	public ViProps setIsolateType() {
		config.setProp(NODE_TYPE, NODE_TYPE_ISOLATE);
		return this;
	}

	public ViProps setInProcessType() {
		config.setProp(NODE_TYPE, NODE_TYPE_IN_PROCESS);
		return this;
	}

	public ViProps setLocalType() {
		config.setProp(NODE_TYPE, NODE_TYPE_LOCAL);
		return this;
	}

	public ViProps setRemoteType() {
		config.setProp(NODE_TYPE, NODE_TYPE_REMOTE);
		return this;
	}
	
}
