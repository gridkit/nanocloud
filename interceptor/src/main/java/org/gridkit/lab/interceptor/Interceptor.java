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
package org.gridkit.lab.interceptor;

import java.io.Serializable;

import org.gridkit.vicluster.ViNode;

/**
 * Handler of execution interception event.
 * Class implementing this interface may witness or override behaviour of intercepted call.
 * <p>
 * {@link Serializable} implementation is required, because instance will be invoked in {@link ViNode} context.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Interceptor extends Serializable {

    public void handle(Interception call);

}
