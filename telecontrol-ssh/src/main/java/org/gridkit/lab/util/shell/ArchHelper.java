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
package org.gridkit.lab.util.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchHelper {

    public static void uncompressZip(File archive, File destination) throws IOException {
        try (ZipFile file = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> n = file.entries();
            while(n.hasMoreElements()) {
                ZipEntry entry = n.nextElement();
                File f = new File(destination, entry.getName());
                FileOutputStream fos = new FileOutputStream(f);
                copy(file.getInputStream(entry), fos);
                fos.close();
            }
        }
    }

    public static void uncompressGz(File archive, File destination) throws IOException {
        GZIPInputStream gzin = new GZIPInputStream(new FileInputStream(archive));
        FileOutputStream fos = new FileOutputStream(destination);
        copy(gzin, fos);
        fos.close();
        gzin.close();
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buf = new byte[1 << 12];
            while(true) {
                int n = in.read(buf);
                if(n >= 0) {
                    out.write(buf, 0, n);
                }
                else {
                    break;
                }
            }
        } finally {
            try {
                in.close();
            }
            catch(Exception e) {
                // ignore
            }
        }
    }
}
