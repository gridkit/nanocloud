package org.gridkit.zerormi;

import java.io.IOException;

class RmiMarshalStack implements RmiMarshaler {

	private final RmiMarshaler[] marshalers;
	
	public RmiMarshalStack(RmiMarshaler... marshalers) {
		this.marshalers = marshalers;
	}

	@Override
	public Object writeReplace(Object obj) throws IOException {
		for(int i = 0; i != marshalers.length; ++i) {
			obj = marshalers[i].writeReplace(obj);
		}
		return obj;
	}

	@Override
	public Object readResolve(Object obj) throws IOException {
		for(int i = marshalers.length - 1; i >= 0; --i) {
			obj = marshalers[i].readResolve(obj);
		}
		return obj;
	}
}
