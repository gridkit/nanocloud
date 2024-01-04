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

import static org.gridkit.nanocloud.VX.LOCAL;

import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.vicluster.ViNode;
import org.junit.After;
import org.junit.Before;

public class Engine2_LocalInstrumentationFeatureTest extends InstrumentationFeatureTest {

    @Override
    @Before
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(LOCAL);
    }

    @Override
    @After
    public void dropCloud() {
        cloud.shutdown();
    }

    @Override
    protected ViNode node(String name) {
        return cloud.node(name);
    }
}
