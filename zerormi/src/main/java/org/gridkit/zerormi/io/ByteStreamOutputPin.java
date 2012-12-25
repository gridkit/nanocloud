package org.gridkit.zerormi.io;


public interface ByteStreamOutputPin extends Pin {

	public boolean isActiveModeSupported();

	public boolean isPassiveModeSupported();
	
	/**
	 * Passive mode linking. 
	 */
	public ByteStreamSink linkAsSink();
	
	/**
	 * Active mode linking. 
	 */
	public void linkToSource(ByteStreamSource source);		
	
	public static class SinkWrapper implements ByteStreamOutputPin {

		private final ByteStreamSink sink;
		private boolean bound; 
		
		public SinkWrapper(ByteStreamSink sink) {
			this.sink = sink;
		}

		@Override
		public boolean isBound() {
			return bound;
		}

		@Override
		public boolean isActive() {
			return false;
		}

		@Override
		public boolean isActiveModeSupported() {
			return false;
		}

		@Override
		public boolean isPassiveModeSupported() {
			return true;
		}

		@Override
		public synchronized ByteStreamSink linkAsSink() {
			if (bound) {
				throw new IllegalArgumentException("Already bound");
			}
			else {
				bound = true;
				return sink;
			}
		}

		@Override
		public void linkToSource(ByteStreamSource source) {
			throw new UnsupportedOperationException();
		}
	}	
}
