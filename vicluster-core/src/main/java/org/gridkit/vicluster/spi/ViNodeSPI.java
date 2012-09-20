package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.ViNode;

public interface ViNodeSPI {
	
	public ViNode getNode();
	
	public void applyPerc(ViPerc perc);

}
