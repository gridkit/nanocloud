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

import java.util.Collection;

import org.gridkit.nanocloud.ViConfExtender;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ViNodeSet {

    /**
     * Return node by name (or group of nodes for pattern).
     */
    public ViNode node(String namePattern);

    public ViNode nodes(String... namePatterns);

    /**
     * Syntax sugar for
     * <code>
     * 		code.nodes("**").x(...);
     * </code>
     */
    public <X> X x(ViConfExtender<X> extender);

    /**
     * List non-terminated nodes matching namePattern
     */
    public Collection<ViNode> listNodes(String namePattern);

    public void shutdown();

}
