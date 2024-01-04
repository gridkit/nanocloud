package org.gridkit.nanocloud.viengine;

import java.util.List;

public interface ConfMap {

    public boolean isPresent(String key);

    public <T> T get(String key);

    public List<String> match(String glob);

}
