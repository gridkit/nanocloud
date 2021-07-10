package org.gridkit.nanocloud.telecontrol;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.gridkit.vicluster.telecontrol.FileBlob;

/**
 * Minimal interface allowing remote execution of slave JVM processes.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface HostControlConsole {

    /**
     * @return Name of host for this console, can be used for informational purposes only.
     */
    public String getHostname();

    /**
     * @return <code>true</code> if console shares file system with master process
     */
    public boolean isLocalFileSystem();

    /**
     * @return remote path for binary resource (copy if needed)
     */
    public String cacheFile(FileBlob blob);

    /**
     * @return remote paths for binary resources (copy if needed)
     */
    public List<String> cacheFiles(List<? extends FileBlob> blobs);

    public Destroyable openSocket(SocketHandler handler);

    public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler);

    public void terminate();

    public interface Destroyable {

        public void destroy();

    }

    public interface ProcessHandler {

        public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr);

        /**
         * Starting command has failed. Additional information could be read from console streams.
         * No finished event will follow.
         *
         * @param error error description (may be dubbed to console streams)
         */
        public void execFailed(OutputStream stdIn, InputStream stdOut, InputStream stdErr, String error);

        public void finished(int exitCode);

    }

    public interface SocketHandler {

        public void bound(String host, int port);

        public void accepted(String remoteHost, int remotePort, InputStream soIn, OutputStream soOut);

        public void terminated(String message);
    }
}
