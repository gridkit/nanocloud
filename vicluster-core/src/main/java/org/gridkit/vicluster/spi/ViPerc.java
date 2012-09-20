package org.gridkit.vicluster.spi;

public interface ViPerc {

	public Class<? extends ViNodeSPI>[] getTargetSPIs();
	
	/**
	 * @param spi - <code>null</code> means no more SPI remains
	 * @return true if perc were applied and can be throw away, false if further perc processing required.
	 */
	public boolean apply(ViNodeSPI spi);
	
}
