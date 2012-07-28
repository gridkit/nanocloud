package org.gridkit.gatling.firegrid;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WorkStream implements Serializable {

	private String workStreamId;
	private WorkExecutor executor;
	private WorkStreamCoordinator coordinator;
	private int threaCount;
	private int packetPrefeatch;
	
	public WorkStream() {
	}
	
	public WorkStream(String workStreamId, WorkExecutor executor, WorkStreamCoordinator coordinator, int threaCount, int packetPrefeatch) {
		this.workStreamId = workStreamId;
		this.executor = executor;
		this.coordinator = coordinator;
		this.threaCount = threaCount;
		this.packetPrefeatch = packetPrefeatch;
	}

	public String getWorkStreamId() {
		return workStreamId;
	}

	public void setWorkStreamId(String workStreamId) {
		this.workStreamId = workStreamId;
	}

	public WorkExecutor getExecutor() {
		return executor;
	}

	public void setExecutor(WorkExecutor executor) {
		this.executor = executor;
	}

	public WorkStreamCoordinator getCoordinator() {
		return coordinator;
	}

	public void setCoordinator(WorkStreamCoordinator coordinator) {
		this.coordinator = coordinator;
	}

	public int getThreaCount() {
		return threaCount;
	}

	public void setThreaCount(int threaCount) {
		this.threaCount = threaCount;
	}

	public int getPacketPrefeatch() {
		return packetPrefeatch;
	}

	public void setPacketPrefeatch(int packetPrefeatch) {
		this.packetPrefeatch = packetPrefeatch;
	}
}
