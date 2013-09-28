/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.zerormi.zlog;

import org.gridkit.zerormi.zlog.PrintStreamLogStream;
import org.junit.Test;

public class PrintStreamLogTest {

	@Test
	public void print_log() {
		
		PrintStreamLogStream ps = new PrintStreamLogStream("%1$tF %1$tT.%1$tL%1$tz TEST %2$s", System.out, true);
		
		ps.log("Message");
		ps.log(new RuntimeException());
		ps.log("Having troubles with %s", "XXX", new RuntimeException());
		ps.log("Having troubles with %s got %s", "AAA", new RuntimeException());
		
	}
	
}
