package org.gridkit.nanocloud.interceptor.misc;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutionException;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.gridkit.lab.interceptor.Interception;
import org.gridkit.lab.interceptor.Interceptor;
import org.gridkit.nanocloud.ViConfigurable;
import org.gridkit.nanocloud.interceptor.Intercept;
import org.gridkit.nanocloud.jmx.ReadThroughMBeanServer;

public class PlatformMBeanServerInterceptor implements Interceptor, Serializable {

    private static final long serialVersionUID = 20140112L;

    public static void apply(ViConfigurable config) {
        Intercept.callSite()
            .onTypes(ManagementFactory.class)
            .onMethod("getPlatformMBeanServer")
            .doInvoke(new PlatformMBeanServerInterceptor())
            .apply(config);
    }

    private transient ReadThroughMBeanServer proxyServer;

    @Override
    public synchronized void handle(Interception call) {
        try {
            if (proxyServer == null) {
                MBeanServer back = (MBeanServer) call.call();
                MBeanServer front = MBeanServerFactory.newMBeanServer();
                proxyServer = new ReadThroughMBeanServer(back, front);
            }
            call.setResult(proxyServer);
        } catch (ExecutionException e) {
            // ignore
        }
    }
}
