package org.gridkit.nanocloud.viengine;

import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.CloudContext.ServiceKey;
import org.gridkit.vicluster.CloudContext.ServiceProvider;

public class SharedEntity<T> implements LazyPragma {

    private final ServiceKey<T> key;
    private final ServiceProvider<T> provider;

    public SharedEntity(ServiceKey<T> key, ServiceProvider<T> provider) {
        this.key = key;
        this.provider = provider;
    }

    @Override
    public Object resolve(String pragmaKey, PragmaReader context) {
        CloudContext ctx = (CloudContext) context.get(Pragma.NODE_CLOUD_CONTEXT);
        if (ctx == null) {
            throw new IllegalArgumentException("Cloud context is missing");
        }
        return provider == null ? ctx.lookup(key) : ctx.lookup(key, provider);
    }

    public String toString() {
        return "SHARED[" + key + "]";
    }
}
