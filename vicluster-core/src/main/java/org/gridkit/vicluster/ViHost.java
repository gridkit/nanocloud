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

import java.util.Map;


/**
 * {@link ViHost} represents a service providing {@link ViNode}. It cloud be in-JVM nodes or real JVMs.
 * 
 * Some {@link ViHost}s support on-demand node configuration: you obtain node first then configure it then start. But other may require for configuration up front, so it is generally advisable to use {@link #allocate(String, ViNodeConfig)} method to obtain node object.
 * 
 * @author Alexey Ragozin
 */
public interface ViHost {
	
	public ViNode allocate(String nodeName, ViNodeConfig config);

	public ViNode get(String nodeName);
	
	public Map<String, ViNode> listNodes();
	
	public void shutdown();
}
