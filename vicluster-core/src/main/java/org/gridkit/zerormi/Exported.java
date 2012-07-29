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
package org.gridkit.zerormi;

/**
 * Special class used to mark object to be exported and replaced by stub.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class Exported {

	private Class<?>[] interfaces;
	private Object object;
	
	public Exported(Object object, Class<?>... interfaces) {
		this.object = object;
		this.interfaces = interfaces;
	}

	public Class<?>[] getInterfaces() {
		return interfaces;
	}

	public Object getObject() {
		return object;
	}
}
