package org.gridkit.gatling.firegrid;

import java.io.Serializable;

@SuppressWarnings("serial")
public class WorkPacket implements Serializable {

	private double executionRate;
	private int packetSeqNo;
	private int packetSize;
	
	public WorkPacket(double executionRate, int packetSize, int packetSeqNo) {
		this.executionRate = executionRate;
		this.packetSize = packetSize;
	}

	public double getExecutionRate() {
		return executionRate;
	}

	public int getPacketSize() {
		return packetSize;
	}

	public int getPacketSeqNo() {
		return packetSeqNo;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
}
