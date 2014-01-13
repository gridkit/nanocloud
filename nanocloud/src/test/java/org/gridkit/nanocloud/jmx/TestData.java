package org.gridkit.nanocloud.jmx;

public class TestData implements TestDataMBean {

	private String name;

	public TestData(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
