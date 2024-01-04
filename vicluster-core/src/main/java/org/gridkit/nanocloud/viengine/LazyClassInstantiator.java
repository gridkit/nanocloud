package org.gridkit.nanocloud.viengine;

class LazyClassInstantiator implements LazyPragma {

    public static LazyClassInstantiator INSTANCE = new LazyClassInstantiator();

    @Override
    public Object resolve(String key, PragmaReader context) {
        int c = key.lastIndexOf(':');
        if (c < 0) {
            throw new IllegalArgumentException("Invalid key '" + key + "', cannot derive class name");
        }
        String cn = key.substring(c + 1);
        Object bean;
        try {
            Class<?> cl = Thread.currentThread().getContextClassLoader().loadClass(cn);
            bean = cl.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot instantiate '" + key + "'", e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot instantiate '" + key + "'", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate '" + key + "'", e);
        }
        return bean;
    }
}
