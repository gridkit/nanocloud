package org.gridkit.util.vicontrol;

public class ViProps {

	/**
	 * Type of ViNode (e.g. isolate, local ...). Types have associated provider.
	 */
	public static final String NODE_TYPE = "node:type";
	
	/**
	 * Name of node.
	 */
	public static final String NODE_NAME = "node:name";

	/**
	 * Arbitrary UID of ViNode. May be used by certain providers.
	 */	
	public static final String NODE_UID = "node:uid";

	/**
	 * Host of which node is meant to run.
	 */	
	public static final String HOST = "node:host";
	
}
