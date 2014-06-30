package org.gridkit.nanocloud.viengine;

public interface PragmaWriter extends PragmaReader {

    public void set(String key, Object value);

    public void setLazy(String key, LazyPragma pragma);

    public void link(String key, String link);
    
}
