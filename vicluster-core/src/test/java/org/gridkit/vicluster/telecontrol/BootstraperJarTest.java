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
package org.gridkit.vicluster.telecontrol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.gridkit.vicluster.telecontrol.bootstraper.Bootstraper;
import org.junit.Assert;
import org.junit.Test;

public class BootstraperJarTest {

	@Test
	public void verify_jar_from_snapshot() throws IOException {
		
	    Manifest mf = new Manifest();	    
	    byte[] data  = ClasspathUtils.createBootstrapperJar(mf, Bootstraper.class);

	    List<String> list = listJar(data);
	    
	    Assert.assertTrue(list.size() > 2);
	    Assert.assertTrue(list.contains(Bootstraper.class.getName().replace('.', '/') + ".class"));
	    Assert.assertTrue(list.contains("META-INF/MANIFEST.MF"));	    
	}

	@Test
	public void verify_jar_from_jar() throws IOException {
	    
	    Manifest mf = new Manifest();	    
	    byte[] data  = ClasspathUtils.createBootstrapperJar(mf, Assert.class);
	    
	    List<String> list = listJar(data);
	    
	    Assert.assertTrue(list.size() > 2);
	    Assert.assertTrue(list.contains(Assert.class.getName().replace('.', '/') + ".class"));
	    Assert.assertTrue(list.contains("META-INF/MANIFEST.MF"));	    
	}

    private List<String> listJar(byte[] data) throws IOException {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
        List<String> list = new ArrayList<String>();
        while(true) {
            ZipEntry ze = zis.getNextEntry();
            if (ze != null) {
                list.add(ze.getName());
            }
            else {
                break;
            }
        }
        return list;
    }	
}
