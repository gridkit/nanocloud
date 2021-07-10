package org.gridkit.nanocloud.viengine;

import java.util.List;

interface PragmaReader {

    public boolean isPresent(String key);

    public <T> T get(String key);

    public String describe(String key);

    public List<String> match(String glob);

    public <T> List<T> collect(String glob, Class<T> type);

    public void copyTo(PragmaWriter writer);

    public void copyTo(PragmaWriter writer, boolean omitExisting);

}
