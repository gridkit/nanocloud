package org.gridkit.zerormi.io;


public interface ByteStreamInputPin extends Pin {
	
	public boolean isActiveModeSupported();

	public boolean isPassiveModeSupported();
	
	/**
	 * Passive linkage.
	 */
	public ByteStreamSource linkAsSource();
	
	/**
	 * Active linkage.
	 */
	public void linkToSink(ByteStreamSink sink);	

	
	public static class SourceWrapper implements ByteStreamInputPin {

		private final ByteStreamSource source;
		private boolean bound; 
		
		public SourceWrapper(ByteStreamSource source) {
			super();
			this.source = source;
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
		public synchronized ByteStreamSource linkAsSource() {
			if (bound) {
				throw new IllegalArgumentException("Already bound");
			}
			else {
				bound = true;
				return source;
			}
		}

		@Override
		public void linkToSink(ByteStreamSink sink) {
			throw new UnsupportedOperationException();
		}
	}
}
