package org.gridkit.gatling.stats;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.gatling.firegrid.DaemonIdentity;

@SuppressWarnings("serial")
public class PerfSampleBlock implements Serializable {

	public Object experiment;
	
	public long startTimestamp;
	public long finishTimestamp;
	
	public int sampleCapacity;
	public AtomicInteger sampleCount = new AtomicInteger();
	
	public DaemonIdentity host;
	
	public Object type;
	public Object[] typeArray;
	public Object[] identityArray;	
	public long[] timestampArray;
	public long[] durationArray;
	public int[] resultArray;
	
	public PerfSampleBlock() {
	}
	
	public PerfSampleBlock(int size) {
		sampleCapacity = size;
		timestampArray = new long[size];
		durationArray = new long[size];
	}
	
	public int size() {
		return sampleCount.get();
	}
	
	public PerfSample getSample(int n) {
		if (n < 0 || n >= sampleCount.get()) {
			throw new ArrayIndexOutOfBoundsException();
		}
		else {
			PerfSample sample = new PerfSample();
			sample.experiment = experiment;
			sample.type = typeArray == null ? type : typeArray[n];
			sample.identity = identityArray == null ? null : identityArray[n];
			sample.timestamp = timestampArray[n];
			sample.duration = durationArray[n];
			sample.result = resultArray == null ? 0 : resultArray[n];
			
			return sample;
		}
	}

	// TODO not really thread safe
	public void addSample(PerfSample sample) {
		int sampleNo;
		while(true) {
			int sampleC = sampleCount.get();
			if (sampleC >= sampleCapacity) {
				throw new IndexOutOfBoundsException();
			}
			else {
				if (!sampleCount.compareAndSet(sampleC, sampleC + 1)) {
					continue;
				}
				else {
					sampleNo = sampleC;
					break;
				}
			}
		}
		// typeArray
		if (typeArray != null) {
			typeArray[sampleNo] = sample.type;
		}
		else if (sampleNo == 0) {
			type = sample.type;
		}
		else if (!equals(type, sample.type)) {
			typeArray = new Object[sampleCapacity];
			for(int i = 0; i != sampleNo; ++i) {
				typeArray[i] = type;
			}
			type = null;
			typeArray[sampleNo] = sample.type;
		}
		
		// identity
		if (identityArray == null && sample.identity != null) {
			identityArray = new Object[sampleNo];
		}
		if (identityArray != null) {
			identityArray[sampleNo] = sample.identity;
		}
		
		// timestamp
		timestampArray[sampleNo] = sample.timestamp;
		
		// duration
		durationArray[sampleNo] = sample.duration;
		
		// result
		if (resultArray == null && sample.result != 0) {
			resultArray = new int[sampleCapacity];
		}
		if (resultArray != null) {
			resultArray[sampleNo] = sample.result;
		}
	}

	private boolean equals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		else {
			return a.equals(b);
		}
	}
}
