package org.gridkit.workshop.coherence;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.tangosol.net.CacheFactory;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings("unchecked")
public class SimpleObjectLoader {
	
	static long start = System.nanoTime();

	static void println() {
	    System.out.println();
	}
	
	static void println(String text) {
	    System.out.println(String.format("[%1$tH:%1$tM:%1$tS.%1$tL] ", System.currentTimeMillis()) + text);
	}
	
	static void setProp(String propertyName, String value) {
		if (!System.getProperties().contains(propertyName)) {
			System.setProperty(propertyName, value);
		}
		System.out.println("[prop] " + propertyName + ": " + System.getProperty(propertyName));
	}

	static Object newInstance(String propertyName) {
		try {
			Class<?> cls = Class.forName(System.getProperty(propertyName));
			return cls.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static int NUMBER_OF_OBJECTS = Integer.getInteger("gridkit.lab.number-of-objects", 900000);
	private static int NUMBER_OF_HOLES = Integer.getInteger("gridkit.lab.number-of-holes", 100000);
	private static int REPORTING_INTERVAL = Integer.getInteger("gridkit.lab.reporting-interval", 100000);
	private static int SLOW_OP_THRESHOLD = Integer.getInteger("gridkit.lab.slow-op-threshold", 50);

	private static int POPULATE_THREAD_COUNT = Integer.getInteger("gridkit.lab.populate.threads", 2);	
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		String cacheName = args[0];
		
		setProp("tangosol.pof.enabled", "true");

		setProp("tangosol.pof.enabled", "true");
	    setProp("tangosol.coherence.cacheconfig", "coherence-lab-cache-config.xml");
	    setProp("tangosol.coherence.distributed.localstorage", "false");

	    setProp("gridkit.lab.object-generator", DefaultTestDocumentGenerator.class.getName());

		
		SimpleObjectLoader cohMiniTest = new SimpleObjectLoader();
		cohMiniTest.start(cacheName);
	}
	
	private AtomicLong size = new AtomicLong();
	private AtomicLong tickCounter = new AtomicLong();
	private long startTime;
	
	private Map<String, Object> store;
	
	public void start(String cacheName) throws InterruptedException, ExecutionException {
		
		System.out.println("Number of objects: " + NUMBER_OF_OBJECTS + "(+ " + NUMBER_OF_HOLES + " holes)");		
		System.out.println("Loading by " + POPULATE_THREAD_COUNT + " thread(s)");		
	
		store = CacheFactory.getCache(cacheName);
		
		final TestDocumentGenerator generator = (TestDocumentGenerator) newInstance("gridkit.lab.object-generator");
		generator.setDocCount(NUMBER_OF_OBJECTS + NUMBER_OF_HOLES);
		
		tickCounter.set(0);
		startTime = System.nanoTime();
		size.set(0);
		ExecutorService threadPool = Executors.newFixedThreadPool(POPULATE_THREAD_COUNT);
		Future<?>[] populateFeatures = new Future[POPULATE_THREAD_COUNT];
		for(int i = 0; i != POPULATE_THREAD_COUNT; ++i) {
			populateFeatures[i] = threadPool.submit(new Runnable() {
				@Override
				public void run() {
					populate(generator, new Random());
				}
			});
		}

		for(int i = 0; i != POPULATE_THREAD_COUNT; ++i) {
			populateFeatures[i].get();
		}

		println("Data loading complete");
		println("Real size: " + store.size());
		println("Loading time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) / 1000d + " (sec)");
		System.exit(0);
	}

	private void populate(TestDocumentGenerator generator, Random rnd) {
		long startTime = System.nanoTime();
		Map<String, Object> buffer = new HashMap<String, Object>();
		while(store.size() < NUMBER_OF_OBJECTS) {
			
			long ss = System.nanoTime();
			
			while(buffer.size() < 100) { 
				int id = rnd.nextInt(NUMBER_OF_OBJECTS + NUMBER_OF_HOLES);
				String key = randomKey(id);
				Object doc = generator.getDoc(id);
				buffer.put(key, doc);
			}
			
			store.putAll(buffer);
			buffer.clear();
			
			long st = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ss);
			if (st > SLOW_OP_THRESHOLD ) {
				println("Slow operation (population): " + st + "ms");
			}

			long nt = tickCounter.incrementAndGet();
			
			if (nt % (REPORTING_INTERVAL / 100) == 0) {
				long time = System.nanoTime() - startTime;
				println("Population iteration " + tickCounter);
				double pace = (double)REPORTING_INTERVAL / time * TimeUnit.SECONDS.toNanos(1);
				println("Population 'put' speed: " + pace + " (" + POPULATE_THREAD_COUNT + " threads)");
				long memUsage = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory();
				println("Cache size: " + store.size() + " (" + NUMBER_OF_OBJECTS + ")");				
				println("Mem usage: " + (memUsage >> 10) + "k");				
				startTime = System.nanoTime();
			}
		}
	}

//	private Object storeGet(String key) {
//		return store.get(key);
//	}
//
//	private void storePut(String key, String value) {
//		store.put(key, value);
//	}
//
//	private void storeRemove(String key) {
//		store.remove(key);
//	}

//	private String randomKey(Random rnd) {
//		long key = 10000000 + rnd.nextInt(NUMBER_OF_OBJECTS + NUMBER_OF_HOLES);
//		return String.valueOf(key);
//	}

	private String randomKey(int id) {
		long key = 10000000 + id;
		return String.valueOf(key);
	}
}
