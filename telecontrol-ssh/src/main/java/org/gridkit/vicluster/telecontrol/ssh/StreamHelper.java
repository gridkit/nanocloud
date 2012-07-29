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
package org.gridkit.vicluster.telecontrol.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class StreamHelper {
	
	public static String digest(byte[] data, String algorithm) {
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			byte[] digest = md.digest(data);
			StringBuilder buf = new StringBuilder();
			for(byte b: digest) {
				buf.append(Integer.toHexString(0xF & (b >> 4)));
				buf.append(Integer.toHexString(0xF & (b)));
			}
			return buf.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static String toString(InputStream is) throws IOException {
		try {
			StringBuilder buf = new StringBuilder();
			Reader reader = new InputStreamReader(is);
			char[] swap = new char[1024];
			while(true) {
				int n = reader.read(swap);
				if (n < 0) {
					break;
				}
				else {
					buf.append(swap, 0, n);
				}
			}
			return buf.toString();
		}
		finally {
			try {
				is.close();
			}
			catch(Exception e) {
				// ignore
			}
		}
	}

	public static Collection<String> toLines(InputStream is) throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			List<String> result = new ArrayList<String>();
			while(true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				result.add(line);
			}
			return result;
		}
		finally {
			try {
				is.close();
			}
			catch(Exception e) {
				// ignore
			}
		}
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
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
