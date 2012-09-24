package org.gridkit.vicluster.spi;


class Any {

	@SuppressWarnings("unchecked")
	public static <V> V cast(Object obj) {
		return (V)obj;
	}
	
    public static void throwUncheked(Throwable e) {
        Any.<RuntimeException>throwAny(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }    
}