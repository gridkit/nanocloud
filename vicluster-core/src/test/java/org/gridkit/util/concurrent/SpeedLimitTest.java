package org.gridkit.util.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;
import org.junit.Assert;
import org.junit.Test;


public class SpeedLimitTest {

	@Test
	public void test_UltraHighRate_1_thread() {
		runTest(10000, 1, false);
	}

	@Test
	public void test_HighRate_1_thread() {
		runTest(1000, 1, false);
	}

	@Test
	public void test_MidRate_1_thread() {
		runTest(100, 1, false);
	}	

	@Test
	public void test_LowRate_1_thread() {
		runTest(5, 1, false);
	}	
	
	@Test
	public void test_SlowestRate_1_thread() {
		runTest(0.5, 1, false);
	}	
	
	@Test
	public void test_UltraHighRate_16_thread() {
		runTest(10000, 16, false);
		runTest(10000, 16, true);
	}

	@Test
	public void test_HighRate_16_thread() {
		runTest(1000, 16, false);
		runTest(1000, 16, true);
	}

	@Test
	public void test_MidRate_16_thread() {
		runTest(100, 16, false);
		runTest(100, 16, true);
	}

	@Test
	public void test_LowRate_16_thread() {
		runTest(5, 16, false);
		runTest(5, 16, true);
	}

	@Test
	public void test_SlowestRate_16_thread() {
		runTest(0.5, 16, false);
	}

	@Test
	public void test_UltraHighRate_4_thread() {
		runTest(10000, 4, false);
		runTest(10000, 4, true);
	}
	
	@Test
	public void test_HighRate_4_thread() {
		runTest(1000, 4, false);
		runTest(1000, 4, true);
	}

	@Test
	public void test_MidRate_4_thread() {
		runTest(100, 4, false);
		runTest(100, 4, true);
	}

	@Test
	public void test_LowRate_4_thread() {
		runTest(5, 4, false);
		runTest(5, 4, true);
	}

	@Test
	public void test_SlowestRate_4_thread() {
		runTest(0.5, 4, false);
	}
	
	private void assertError(double value, double target, double tolerance) {
		Assert.assertTrue(String.format("%f within %.3f bounds from %f", value, tolerance, target), value < (target + target * tolerance) && value > (target - target * tolerance));
	}
	
	private void runTest(double targetRate, int threads, boolean balanced) {
		BlockingBarrier limit = Barriers.speedLimit(targetRate);
		double rate = balanced ?
					testSpeedLimit_balanced(limit, threads, (int)(targetRate * 10)):
					testSpeedLimit_unbalanced(limit, threads, (int)(targetRate * 10));
		System.out.println(String.format("Thread %2d, %s, target rate %f -> %f (error %.3f%%)", threads, balanced ? "  balanced" : "unbalanced" , targetRate, rate, Math.abs(100 * (targetRate - rate) / targetRate)));
		assertError(rate, targetRate, 0.05);
	}

	private double testSpeedLimit_balanced(final BlockingBarrier limit, int threadCount, int events) {
		long start = System.nanoTime();
		final CountDownLatch barrier = new CountDownLatch(threadCount);
		final int eventsPerThread = events / threadCount;
		
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		for(int i = 0; i != threadCount; ++i) {
			executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws InterruptedException, BrokenBarrierException {
					for(int i = 0; i != eventsPerThread; ++i) {
						limit.pass();
					}
					barrier.countDown();
					return null;
				}
			});
		}
		
		try {
			barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long time = System.nanoTime() - start;
		executor.shutdown();

		double rate = 1d * TimeUnit.SECONDS.toNanos(1) * eventsPerThread * threadCount / time;

		return rate;		
	}

	private double testSpeedLimit_unbalanced(final BlockingBarrier limit, int threadCount, int events) {
		
		long start = System.nanoTime();
		final AtomicInteger counter = new AtomicInteger(events + 1);
		final CountDownLatch finishBarrier = new CountDownLatch(threadCount);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		for(int i = 0; i != threadCount; ++i) {
			executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws InterruptedException, BrokenBarrierException {
					while(true) {
						if (counter.decrementAndGet() < 0) {
							break;
						}
						else {
							limit.pass();
						}
					}
					finishBarrier.countDown();
					return null;
				}
			});
		}
		
		try {
			finishBarrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long time = System.nanoTime() - start;
		executor.shutdown();

		double rate = 1d * TimeUnit.SECONDS.toNanos(1) * events / time;

		return rate;		
	}
	
	public static void main(String[] args) {
		SpeedLimitTest test = new SpeedLimitTest();
		
		test.test_UltraHighRate_1_thread();
		test.test_HighRate_1_thread();
		test.test_MidRate_1_thread();
		test.test_LowRate_1_thread();
		test.test_SlowestRate_1_thread();

		test.test_UltraHighRate_16_thread();
		test.test_HighRate_16_thread();
		test.test_MidRate_16_thread();
		test.test_LowRate_16_thread();
		test.test_SlowestRate_16_thread();
		
		test.test_UltraHighRate_4_thread();
		test.test_HighRate_4_thread();
		test.test_MidRate_4_thread();
		test.test_LowRate_4_thread();
		test.test_SlowestRate_4_thread();
	}
}
