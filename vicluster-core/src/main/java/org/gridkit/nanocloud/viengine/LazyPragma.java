package org.gridkit.nanocloud.viengine;

interface LazyPragma {

    public Object resolve(String key, PragmaReader context);
    
}
