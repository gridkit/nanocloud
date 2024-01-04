package org.gridkit.nanocloud.agent;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;

/**
 * Common part of agent management.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public abstract class AbstractNanoAgent {

    private final AtomicInteger counter = new AtomicInteger();
    private List<Session> sessions = new ArrayList<Session>();

    protected synchronized Closeable start(String remoteInfo, InputStream in, OutputStream out) {

        String id = "" + counter.getAndIncrement();
        Session session = new Session(id, remoteInfo, new Tunneller(), in, out);
        sessions.add(session);

        return session;
    }

    protected synchronized void cleanUpSession(Session session) {
        sessions.remove(session);
    }

    private class Session implements Runnable, Closeable {

        @SuppressWarnings("unused")
        private final String remoteInfo;
        @SuppressWarnings("unused")
        private final String id;
        private final Tunneller tunneller;
        private final InputStream in;
        private final OutputStream out;
        private final ThreadGroup tgroup;
        private volatile boolean terminated;

        protected Session(String id, String remoteInfo, Tunneller tunneller, InputStream in, OutputStream out) {
            this.id = id;
            this.remoteInfo = remoteInfo;
            this.tunneller = tunneller;
            this.in = in;
            this.out = out;
            this.tgroup = new ThreadGroup(id);

            Thread pthread = new Thread(tgroup, this);
            pthread.setName("CTRL-" + id);
            pthread.start();
        }

        @Override
        public void run() {
            try {
                tunneller.process(in, out);
            } finally {
                cleanUpSession(this);
            }
        }

        @Override
        public void close() {
            if (!terminated) {
                // race condition is ok, we want to prevent enternal recursion
                terminated = true;
                try {
                    in.close();
                } catch (Exception e) {
                    // ignore
                }
                try {
                    out.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }
}
