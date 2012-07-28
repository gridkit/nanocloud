package org.gridkit.gatling.stats;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PerfSample implements Serializable {

	public Object experiment;
	public Object type;
	public Object identity; // optional
	public long timestamp;
	public long duration;
	public int result; // 0 is default
	
}
