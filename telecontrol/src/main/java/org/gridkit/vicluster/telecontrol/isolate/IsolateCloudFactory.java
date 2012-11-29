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
package org.gridkit.vicluster.telecontrol.isolate;

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;

public class IsolateCloudFactory {

	public static ViManager createCloud(String... packages) {
		
		IsolateJvmNodeFactory factory = new IsolateJvmNodeFactory(packages);
		JvmNodeProvider provider = new JvmNodeProvider(factory);
		
		return new ViManager(provider);		
	}
	
}
