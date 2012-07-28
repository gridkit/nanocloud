package org.gridkit.gatling.firegrid;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.gridkit.gatling.stats.PerfSample;
import org.gridkit.gatling.stats.PerfSampleBlock;
import org.gridkit.util.concurrent.Barriers;
import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.formating.Formats;

public abstract class AbstractMasterServer implements MasterCoordinator {

	private Registry rmiRegistry;
	private Map<DaemonIdentity, Slave> slaves = new ConcurrentHashMap<DaemonIdentity, Slave>(); 
	private Thread pinger;
	
	public void start() throws RemoteException, AlreadyBoundException {
		rmiRegistry = LocateRegistry.createRegistry(1099);
		rmiRegistry.bind("master", RmiHelper.exportObject(this));
		pinger = new Thread() {
			@Override
			public void run() {
				runPinger();
			}
		};
		pinger.setDaemon(true);
		pinger.setName("Slave pinger");
		pinger.start();
	}

	protected void runPinger() {
		try {
			BlockingBarrier limit = Barriers.speedLimit(2);
			while(true) {
				Thread.sleep(5000);
				for(Slave slave: slaves.values()) {
					limit.pass();
					try {
						slave.ping();
					} catch (RemoteException e) {
						System.err.println(Formats.currentDatestamp() + " Slave ping error");
						System.exit(0);
					}
				}
			}		
		} catch (InterruptedException e) {
			// ignore
		} catch (BrokenBarrierException e) {
			// ignore
		}
	}

	@Override
	public void swear(Slave slave) throws RemoteException {
		slaves.put(slave.getIdentity(), slave);
		System.out.println(Formats.currentDatestamp() + " Slave has connected " + slave.getIdentity() + " (" + slaves.size() + " slaves connected)");
		onNewSlave(slave);
	}

	@Override
	public void ping() throws RemoteException {
		// do nothing
	}

	public void awaitSlaves(int slaveNumber) {
		System.out.println(Formats.currentDatestamp() + " Waiting for slaves to connect ...");
		while(true) {
			if (slaves.size() >= slaveNumber) {
				return;
			}
			else {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	public WorkStreamSession createWorkstream(String id, WorkExecutor executor, int slaveThreadCount) throws RemoteException {
		ServerWorkstreamSession session = new ServerWorkstreamSession(10 ,slaves.size());
		WorkExecutor me = new MeasuringWorkExecutor(executor, session);
		
		WorkStream ws = new WorkStream(id, me, RmiHelper.exportObject(session), slaveThreadCount, 0);
		
		for(Slave slave: slaves.values()) {
			try {
				slave.createWorkStream(ws);
			} catch (RemoteException e) {
				throw new RuntimeException(e);
			}
		}
		
		SampleDumper dumper = new SampleDumper(id, id + "-" + Formats.currentFileDatestamp(), session, slaves.size());
		Thread thread = new Thread(dumper);
		thread.setName("SampleDumper-" + id);
		thread.start();
		
		session.setDumper(thread);
		
		return session;
	}
	
	protected void onNewSlave(Slave slave) {
		// do nothing
	}
	
	public interface WorkStreamSession {
		
		public void setRate(double rate);
		
		public void stop();

		public abstract void setWorkPacketTimeSlice(int millis);

		public abstract void setWorkPacketFixedSize(int size);
		
		public void waitForTime(long millis) throws InterruptedException;
		
		public void waitForPackets(int packetNumber) throws InterruptedException;
		
	}
	
	private class ServerWorkstreamSession implements WorkStreamSession, WorkStatisticsReceiver, WorkStreamCoordinator {
		
		private BlockingDeque<PerfSampleBlock> statistics = new LinkedBlockingDeque<PerfSampleBlock>();
		private volatile List<ExperimentPhase> phases = new ArrayList<AbstractMasterServer.ExperimentPhase>();
		private Map<DaemonIdentity, ExperimentPhase> slaveState = new ConcurrentHashMap<DaemonIdentity, AbstractMasterServer.ExperimentPhase>();
		private AtomicInteger packetSeqNo = new AtomicInteger();
		private volatile boolean stoped = false;
		
		private int workPacketFixedSize = 0;
		private int workPacketTimeSlice = 2000;
		private int slaveCount;
		private Thread dumper;
		
		private ServerWorkstreamSession(int workPacketTimeSlice, int slaveCount) {
			this.slaveCount = slaveCount;
			this.workPacketTimeSlice = workPacketTimeSlice;
		}

		public void setDumper(Thread thread) {
			this.dumper = thread;
		}

		void switchToNextPhase(ExperimentPhase phase) throws InterruptedException {
			System.out.println("Switching to phase: " + phase);
			phase.initiationTimestamp = System.currentTimeMillis();
			phase.barrier = new CountDownLatch(slaveCount);
			
			List<ExperimentPhase> newTimeline = new ArrayList<AbstractMasterServer.ExperimentPhase>(phases.size() + 1);
			newTimeline.add(phase);
			newTimeline.addAll(phases);
			
			phases = newTimeline;
			phase.barrier.await();
			phase.confirmationTimestamp = System.currentTimeMillis();
			phase.barrier = null;
			System.out.println("Phase has been switched to: " + phase);
		}
		
		@Override
		public WorkPacket fetchWorkPacket(DaemonIdentity identity) {
			while(true) {
				ExperimentPhase phase = phases.isEmpty() ? null : phases.get(0);
				if (phase == null) {
					LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(300));
					continue;
				}
				
				ExperimentPhase oldPhase = slaveState.get(identity);
				if (oldPhase != phase) {
					phase.barrier.countDown();
					slaveState.put(identity, phase);
				}
				
				int packetSize = workPacketFixedSize != 0 ? workPacketFixedSize : (int)(phase.rate * workPacketTimeSlice);
				return phase.terminate ? null : new WorkPacket(phase.rate, packetSize, packetSeqNo.incrementAndGet());
			}
		}

		@Override
		public void sendStatistics(PerfSampleBlock block) throws RemoteException {
			System.out.println(Formats.currentDatestamp() + " Got statistics block from " + block.host);
			statistics.add(block);			
		}

		@Override
		public void setRate(double rate) {
			ExperimentPhase phase = new ExperimentPhase();
			phase.rate = rate / slaveCount;
			try {
				switchToNextPhase(phase);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void waitForTime(long millis) throws InterruptedException {
			long deadline = System.currentTimeMillis() + millis;
			while(true) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining < 0) {
					break;
				}
				else {
					Thread.sleep(remaining);
				}
			}
		}

		@Override
		public void waitForPackets(int packetNumber) throws InterruptedException {
			while(packetSeqNo.get() < packetNumber) {
				waitForTime(300);
			}
		}

		@Override
		public void setWorkPacketFixedSize(int size) {
			this.workPacketFixedSize = size;
		}

		@Override
		public void setWorkPacketTimeSlice(int millis) {
			this.workPacketTimeSlice = millis;
		}

		@Override
		public void stop() {
			ExperimentPhase phase = new ExperimentPhase();
			phase.terminate = true;
			try {
				switchToNextPhase(phase);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			stoped = true;
			
			if (dumper != null) {
				try {
					dumper.join();
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	private static final class ExperimentPhase {
		
		boolean terminate;
		double rate;
		CountDownLatch barrier;
		long initiationTimestamp;
		long confirmationTimestamp;
		
		@Override
		public String toString() {
			return terminate ? "terminate" : "load rate: " + rate + " op/s";
		}
	}

	private static final class SampleDumper implements Runnable {

		private static final long REPORT_INTERVAL = 30000;
	
		private String id;
		private String fileName;
		
		private ServerWorkstreamSession session;
		private Map<DaemonIdentity, Long> slaveTimestamps = new HashMap<DaemonIdentity, Long>();
		private int slaveCount; 
		
		private Mean streamMean = new Mean();
		private StandardDeviation streamStdDev = new StandardDeviation(); 
		
		private long lastReportTimestamp;
		
		private SampleBuffer buffer = new SampleBuffer();
		
		private int nextPhase = 0;
		
		public SampleDumper(String id, String fileName, ServerWorkstreamSession session, int slaveCount) {
			this.id = id;
			this.fileName = fileName;
			this.session = session;
			this.slaveCount = slaveCount;
		}

		long maxCommonTimestamp() {
			if (slaveTimestamps.size() < slaveCount) {
				return 0;
			}
			else {
				long min = slaveTimestamps.values().iterator().next();
				for(Long ts : slaveTimestamps.values()) {
					if (ts < min) {
						min = ts;
					}
				}
				return min;
			}
		}
		
		@Override
		public void run() {
			try {
				lastReportTimestamp = System.currentTimeMillis();
				while(true) {
					PerfSampleBlock block = session.statistics.pollFirst(500, TimeUnit.MILLISECONDS);
					while(block != null) {
						load(block);
						block = session.statistics.pollFirst();
					}
					
					printInstantStats();
					dumpCompletedPhases();
					
//					System.out.println("Buffer size " + buffer.size());
				}
			} catch (InterruptedException e) {
				System.err.println("Dumper thread was interrupted");
			}			
		}

		private void load(PerfSampleBlock block) {
			long maxTimestamp = 0;

			double scale = 1d / TimeUnit.MILLISECONDS.toNanos(1);
			
			for(int i = 0; i != block.size(); ++i) {
				PerfSample sample = block.getSample(i);
				buffer.add(sample);
				if (sample.timestamp > maxTimestamp) {
					maxTimestamp = sample.timestamp;
				}
				streamMean.increment(scale * sample.duration);
				streamStdDev.increment(scale * sample.duration);
			}
			
			slaveTimestamps.put(block.host, maxTimestamp);
		}
		
		private void printInstantStats() {
			if (lastReportTimestamp + REPORT_INTERVAL < System.currentTimeMillis()) {

				if (streamMean.getN() > 0) {
					long rangeStart = lastReportTimestamp;
					long rangeEnd = System.currentTimeMillis();
					
					
					String line = String.format("  {%s} Thoughput: %.2f op/s, Mean: %.3f ms, StdDev: %.3f ms",
							id,
							1000d * streamMean.getN() / (rangeEnd - rangeStart),
							streamMean.getResult(),
							streamStdDev.getResult());
					
					System.out.println(Formats.currentDatestamp() + line);
				}
				streamMean.clear();
				streamStdDev.clear();
				lastReportTimestamp = System.currentTimeMillis();
			}
		}
		
		private void dumpCompletedPhases() {
			// data from start of buffer until lastReportTimestamp could be dumped
			
			if (buffer.isEmpty()) {
//				if (isTerminal(nextPhase) && (maxCommonTimestamp() + 5000 < System.currentTimeMillis())) {
//					System.out.println("Terminating dumper for {" + id + "}");
//					throw new ThreadDeath();
//				}
				return;
			}
			
			long nextPhaseStart = getPhaseStart(nextPhase);
			if (nextPhaseStart == 0) {
				return;
			}
			else {
//				System.out.println("Phase: " + nextPhase + " oldest:" + Formats.toTimestamp(buffer.firstKey()) + " reported:" + Formats.toTimestamp(lastReportTimestamp) + " phase end:" + Formats.toTimestamp(nextPhaseStart));
				long ceil = maxCommonTimestamp() + 1;
				SampleBuffer range = buffer.range(Long.MIN_VALUE, ceil);
				if (!range.isEmpty()) {
					dumpRange(nextPhase - 1, range);
				}
				else if (isTerminal(nextPhase) && (isStopped())) {
					dumpRange(nextPhase - 1, buffer);
					System.out.println("Terminating dumper for {" + id + "}");
					throw new ThreadDeath();
				}
				
				if (maxCommonTimestamp() <= nextPhaseStart) {
					return;
				}
				else {
					// switching to next phase;
					if (isTerminal(nextPhase)) {
						System.out.println("Terminating dumper for {" + id + "}");
						throw new ThreadDeath();
					}
					else {						
						++nextPhase;
//						System.err.println("Phase " + nextPhase);
						dumpCompletedPhases();
					}
				}					
			}
		}
		
		private long getPhaseStart(int phaseNo) {
			// even - transition period, odd - stable period
			int phaseIndex = phaseNo / 2;
			List<ExperimentPhase> phases = session.phases; 
			if (phaseIndex >= phases.size()) {
//				System.err.println("Phase is not started: " + phaseIndex);
				return 0;
			}
			ExperimentPhase phase = phases.get(phases.size() - phaseIndex - 1);
			if (phaseNo % 2 == 0) {
				return phase.initiationTimestamp;
			}
			else {
				return phase.confirmationTimestamp;
			}
		}
		
		private boolean isStopped() {
			return session.stoped;
		}
		
		private boolean isTerminal(int phaseNo) {
			// even - transition period, odd - stable period
			int phaseIndex = phaseNo / 2;
			List<ExperimentPhase> phases = session.phases; 
			if (phaseIndex >= phases.size()) {
				return false;
			}
			ExperimentPhase phase = phases.get(phases.size() - phaseIndex - 1);
			if (phaseNo % 2 == 0) {
				return phase.terminate;
			}
			else {
				return false;
			}
		}		
		
		private void dumpRange(int phaseNo, SampleBuffer samples) {
			int phaseId = phaseNo / 2;
			String phaseType = phaseNo % 2 == 0 ? "A" : "B";
			String filepart = String.format("%s-%04d%s.samples", 
					fileName,
					phaseId,
					phaseType);
			try {
				FileWriter writer = new FileWriter(new File(filepart), true);
				for(PerfSample sample: samples) {
					dumpSample(writer, sample);
				}
				writer.close();
				System.out.println("Append " + samples.size() + " to '" + filepart + "'");
				samples.clear();
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void dumpSample(FileWriter writer, PerfSample sample) throws IOException {
			String line = String.format(
					"%1$tFT%1$tT.%1$tL%1$tz\t%1d\t%2d\t%3d\n", 
					sample.timestamp,
					sample.duration,
					sample.result
					);
			writer.append(line);			
		}
	}
	
	private static class SampleBuffer implements Iterable<PerfSample> {
		
		private SortedMap<Long, PerfSample[]> data;
		
		public SampleBuffer() {
			this.data = new TreeMap<Long, PerfSample[]>();
		}
		
		public SampleBuffer(SortedMap<Long, PerfSample[]> data) {
			this.data = data;
		}
		
		public boolean isEmpty() {
			return data.isEmpty();
		}

		public void clear() {
			data.clear();
		}
		
		public int size() {
			int size = 0;
			for(PerfSample[] row: data.values()) {
				size += row.length;
			}
			return size;
		}
		
		public SampleBuffer range(long from, long to) {
			return new SampleBuffer(data.subMap(from, to));
		}
		
		public void add(PerfSample sample) {
			PerfSample[] buffer = data.get(sample.timestamp);
			if (buffer == null) {
				buffer = new PerfSample[]{sample};
			}
			else {
				buffer = Arrays.copyOf(buffer, buffer.length + 1);
				buffer[buffer.length - 1] = sample;
			}
			data.put(sample.timestamp, buffer);
		}

		@Override
		public Iterator<PerfSample> iterator() {
			return new Iterator<PerfSample>() {

				Iterator<PerfSample[]> baseIterator = data.values().iterator();
				PerfSample[] buffer;
				int position;
				
				@Override
				public boolean hasNext() {
					return buffer != null || baseIterator.hasNext();
				}

				@Override
				public PerfSample next() {
					if (buffer == null) {
						buffer = baseIterator.next();
					}
					PerfSample sample = buffer[position++];
					if (position >= buffer.length) {
						buffer = null;
						position = 0;
					}
					return sample;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		
		
	}
}
