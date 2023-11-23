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

	private Object object;
	private final Class<?> originalClass;

	public Exported(Object object, Class<?> originalClass) {
		this.object = object;
		this.originalClass = originalClass;
	}

	public Object getObject() {
		return object;
	}

	public Class<?> getOriginalClass() {
		return originalClass;
	}
}
