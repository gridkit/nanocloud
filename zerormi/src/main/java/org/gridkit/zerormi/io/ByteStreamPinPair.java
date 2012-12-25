package org.gridkit.zerormi.io;

public interface ByteStreamPinPair {

	public ByteStreamInputPin getInput();

	public ByteStreamOutputPin getOutput();
	
	public static class Named implements ByteStreamPinPair {
		
		private final String name;
		private final ByteStreamInputPin input;
		private final ByteStreamOutputPin output;
		
		public Named(String name, ByteStreamInputPin input, ByteStreamOutputPin output) {
			this.name = name;
			this.input = input;
			this.output = output;
		}

		@Override
		public ByteStreamInputPin getInput() {
			return input;
		}
		
		@Override
		public ByteStreamOutputPin getOutput() {
			return output;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}	
}
