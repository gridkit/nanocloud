package org.gridkit.nanocloud.viengine;

import java.util.List;

interface PragmaReader extends ConfMap {

    @Override
	public boolean isPresent(String key);

    @Override
	public <T> T get(String key);

    public String describe(String key);

    @Override
	public List<String> match(String glob);

    public <T> List<T> collect(String glob, Class<T> type);

    public void copyTo(PragmaWriter writer);

    public void copyTo(PragmaWriter writer, boolean omitExisting);

}
