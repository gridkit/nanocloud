package org.gridkit.nanocloud.viengine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.vicluster.telecontrol.FileBlob;

public class RemoteControlConnectionManager {

    private ConcurrentMap<RemoteHostConnector, SharedConsole> cache = new ConcurrentHashMap<RemoteHostConnector, SharedConsole>();

    public HostControlConsole open(RemoteHostConnector factory) {
        while (true) {
            if (!cache.containsKey(factory)) {
                cache.putIfAbsent(factory, new SharedConsole(factory));
            }
            SharedConsole con = cache.get(factory);
            if (con == null) {
                continue;
            }
            synchronized(con) {
                if (con.terminated) {
                    continue;
                }
                return con.createDelegate();
            }
        }
    }

    private synchronized void dispose(RemoteHostConnector factory, boolean endOfCloud) {
        SharedConsole console = cache.get(factory);
        synchronized (console) {
            if (console.delegates.isEmpty() || endOfCloud) {
                console.masterConsole.terminate();
                console.terminated = true;
            }
            cache.remove(factory);
        }
    }

    public synchronized void terminate() {
        while (!cache.isEmpty()) {
            RemoteHostConnector ep = cache.keySet().iterator().next();
            dispose(ep, true);
        }
    }

    private class SharedConsole {

        private final RemoteHostConnector factory;

        private HostControlConsole masterConsole;

        private final List<HostControlConsole> delegates = new ArrayList<HostControlConsole>();

        private boolean terminated = false;

        public SharedConsole(RemoteHostConnector factory) {
            this.factory = factory;
        }

        public synchronized HostControlConsole createDelegate() {
            try {
                if (masterConsole == null) {
                    masterConsole = factory.connect();
                }
                HostControlConsoleDelegate delegate = new HostControlConsoleDelegate();
                delegates.add(delegate);
                return delegate;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private class HostControlConsoleDelegate implements HostControlConsole {

            private volatile boolean terminated;

            @Override
            public String getHostname() {
                if (terminated) {
                    throw new IllegalStateException("Terminated");
                }
                return masterConsole.getHostname();
            }

            @Override
            public boolean isLocalFileSystem() {
                if (terminated) {
                    throw new IllegalStateException("Terminated");
                }
                return masterConsole.isLocalFileSystem();
            }

            @Override
            public String cacheFile(FileBlob blob) {
                if (terminated) {
                    throw new IllegalStateException("Terminated");
                }
                return masterConsole.cacheFile(blob);
            }

            @Override
            public List<String> cacheFiles(List<? extends FileBlob> blobs) {
                if (terminated) {
                    throw new IllegalStateException("Terminated");
                }
                return masterConsole.cacheFiles(blobs);
            }

            @Override
            public Destroyable openSocket(SocketHandler handler) {
                if (terminated) {
                    throw new IllegalStateException("Terminated");
                }
                return masterConsole.openSocket(handler);
            }

            @Override
            public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler) {
                if (terminated) {
                    throw new IllegalStateException("Terminated");
                }
                return masterConsole.startProcess(workDir, command, env, handler);
            }

            @Override
            public void terminate() {
                terminated = true;
                boolean dispose = false;
                synchronized (SharedConsole.this) {
                    delegates.remove(this);
                    if (delegates.isEmpty()) {
                        dispose = true;
                    }
                }
                if (dispose) {
                    dispose(factory, false);
                }
            }
        }
    }

}
