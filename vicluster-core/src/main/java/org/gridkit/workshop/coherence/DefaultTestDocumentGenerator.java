package org.gridkit.workshop.coherence;


public class DefaultTestDocumentGenerator extends TestDocumentGenerator {

	public DefaultTestDocumentGenerator() {
		addField("S1-1", 1);
		addField("S1-2", 1);
		addField("S10-1", 10);
		addField("S10-2", 10);
		addField("S100-1", 100);
		addField("S100-2", 100);
		addField("S1000-1", 1000);
		addField("S1000-2", 1000);
		addField("R0.1-1", 0.1, true);
		addField("R0.1-2", 0.1, true);
		addField("R0.5-1", 0.5, true);
		addField("R0.5-2", 0.5, true);
	}	
}
