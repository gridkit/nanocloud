/**
 * Copyright 2008-2009 Grid Dynamics Consulting Services, Inc.
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
package org.gridkit.workshop.coherence;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.ParallelAwareAggregator;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class CoherenceCacheNode {
	
	static void println() {
	    System.out.println();
	}
	
	static void println(String text) {
	    System.out.println(String.format("[%1$tH:%1$tM:%1$tS.%1$tL] ", new Date()) + text);
	}
	
	static void setProp(String propertyName, String value) {
		if (!System.getProperties().contains(propertyName)) {
			System.setProperty(propertyName, value);
		}
		System.out.println("[prop] " + propertyName + ": " + System.getProperty(propertyName));
	}
	
	public static void main(String[] args) {

	    setProp("tangosol.pof.enabled", "true");
	    setProp("tangosol.coherence.cacheconfig", "coherence-lab-cache-config.xml");
	    setProp("tangosol.coherence.distributed.localstorage", "true");
	    
		try {			
			if (args.length == 0) {
				System.err.println("No caches specified");
				System.exit(1);
			}
			for (String arg: args) {
				NamedCache cache = CacheFactory.getCache(arg);
				println("Cache '" + arg + "' has started. Size: " + cache.size());
				
			}			
			while(true) {
				LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(300));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

    private static void checkAccess(NamedCache cache, Filter filter) {

        cache.keySet(filter).size();
        
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        long start = System.nanoTime();
        int i;
        for(i = 1; i != 100; ++i) {
            cache.keySet(filter).size();
            if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(15)) {
                break;
            }
        }
        long finish = System.nanoTime();
      
        System.out.println("Filter time:" + (TimeUnit.NANOSECONDS.toMicros((finish - start) / i) / 1000d) + "(ms) - " + filter.toString());
    };
    
    public static class BinarySizeAggregator implements ParallelAwareAggregator {
        
        
        @Override
        public Object aggregateResults(Collection results) {
            long sum = 0;
            for(Object x : results) {
                sum += ((Number)x).longValue();
            }
            return sum;
        }

        @Override
        public EntryAggregator getParallelAggregator() {
            return this;
        }

        @Override
        public Object aggregate(Set entries) {
            long sum = 0;
            int count = 0;
            for(Object entry : entries) {
                BinaryEntry be = (BinaryEntry) entry;
                sum += be.getBinaryValue().length();
                ++count;
            }
            System.out.println("Measured " + count + " entries, average size is " + 1d * sum / count);
            return sum;
        }
    }
}
