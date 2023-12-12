/**
 * Copyright 2014 Alexey Ragozin
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
package org.gridkit.nanocloud;

/**
 * Few magic properties for internal use only ;)
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class MagicProps {

    /**
     * Adds fixed latency to RMI invocation, useful for testing.
     */
    public static final String DEBUG_RPC_DELAY = "gridkit.zerormi.debug.rpc-delay";

    public static final String ISOLATE_SUPPRESS_MULTIPLEXOR = "gridkit.isolate.suppress.multiplexor";

    public static final String TELECONTROL_VERBOSE = "org.gridkit.telecontrol.verbose";

}
