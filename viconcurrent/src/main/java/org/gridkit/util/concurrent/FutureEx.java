package org.gridkit.util.concurrent;

import java.util.concurrent.Future;


public interface FutureEx<V> extends Future<V> {

	public void addListener(Box<? super V> box);
	
}
