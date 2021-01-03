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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gridkit.nanocloud.testutil.JvmVersionCheck;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.junit.Assume;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressWarnings("deprecation")
public class IsolatePermSpaceLeakTest {

    @Test
    public void test_permanent_leak() {

        // This test is akward and porting to Java 11 has little sense
        Assume.assumeTrue(JvmVersionCheck.isJava8orBelow());

        // 100 is a bit too short, but test should not take forever
        int interations = 500;
        for(int i = 0; i != interations; ++i) {
            ViNode node = new IsolateViNode("node-" + i);
            IsolateViNode.includePackage(node, "org.gridkit");
            // loading XML library to an isolate to put some stress on perm gen
            IsolateViNode.includePackage(node, "org.w3c");
            IsolateViNode.includePackage(node, "javax.xml");
            IsolateViNode.includePackage(node, "com.sun.org.apache.xerces");
            IsolateViNode.includePackage(node, "com.sun.xml");

            node.exec(new VoidCallable() {
                @Override
                public void call() throws Exception {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.newDocument();
                    Element el = doc.createElement("root");
                    el.setAttribute("attr", "value");
                    el.appendChild(doc.createCDATASection("HAHAH"));
                    el.appendChild(doc.createElement("child"));
                    el.appendChild(doc.createTextNode("text"));
                    doc.appendChild(el);

                    doc.normalizeDocument();
                }
            });

            node.shutdown();

            if (i % 100 == 99) {
                for(MemoryPoolMXBean mpool: ManagementFactory.getMemoryPoolMXBeans()) {
                    if (mpool.getName().toUpperCase().contains("PERM")) {
                        System.out.println(mpool.getName() + ": " + (mpool.getUsage().getUsed() >> 20) + "M");
                    }
                }
            }
        }
    }

}
