/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.nanocloud.telecontrol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gridkit.nanocloud.telecontrol.HostControlConsole.Destroyable;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.SocketHandler;
import org.gridkit.nanocloud.viengine.ProcessLifecycleListener;
import org.gridkit.nanocloud.viengine.ProcessLifecycleListener.ExecFailedInfo;
import org.gridkit.nanocloud.viengine.ProcessLifecycleListener.TerminationInfo;
import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ClasspathUtils;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.StreamCopyService.Link;
import org.gridkit.vicluster.telecontrol.bootstraper.SmartBootstraper;
import org.gridkit.zeroio.LookbackOutputStream;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.NamedStreamPair;
import org.gridkit.zerormi.SocketStream;
import org.gridkit.zerormi.hub.SlaveSpore;

/**
 * {@link ProcessSporeLauncher} is using {@link SmartBootstraper}
 * to instantiate and launch slave spore.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class ProcessSporeLauncher implements ProcessLauncher {

    StreamCopyService streamCopyService;

    // Used by reflection
    @Deprecated
    public ProcessSporeLauncher() {
    }

    public ProcessSporeLauncher(StreamCopyService streamCopyService) {
        this.streamCopyService = streamCopyService;
    }

    @Override
    public ManagedProcess launchProcess(LaunchConfig config) {

        HostControlConsole console = config.getControlConsole();
        RemoteExecutionSession rmiSession = config.getRemotingSession();
        List<String> slaveArgs = config.getSlaveArgs();
        Map<String, String> slaveEnv = config.getSlaveEnv();
        String slaveWD = config.getSlaveWorkDir();

        ControlledSession session = new ControlledSession();
        session.session = rmiSession;
        session.streamCopyService = config.getStreamCopyService();

        if (session.streamCopyService == null) {
            session.streamCopyService = streamCopyService;
        }
        if (session.streamCopyService == null) {
            session.streamCopyService = BackgroundStreamDumper.SINGLETON;
        }

        SlaveSpore spore = rmiSession.getMobileSpore();

        // TODO single socket per console should be reused or at least it should be closed after use
        Destroyable socketHandler = console.openSocket(session);
        session.socketHandle = socketHandler;

        InetSocketAddress sockAddr = (InetSocketAddress)fget(session.bindAddress);
        CallbackSporePlanter planter = new CallbackSporePlanter(spore, sockAddr.getHostName(), sockAddr.getPort());
        byte[] binspore = serialize(planter);
        session.binspore = binspore;

        String javaCmd = config.getSlaveJvmExecCmd();
        String bootstraper = null;
        String shallowClasspath = null;

        if (config.getSlaveShallowClasspath() != null && !config.getSlaveShallowClasspath().isEmpty()) {
            if (!console.isLocalFileSystem()) {
                throw new IllegalArgumentException("Shallow classptah cannot be used to remote nodes");
            }
            shallowClasspath = makeCpLine(config.getSlaveShallowClasspath());
        }
        else {
            bootstraper = buildBootJar(console, config.getSlaveClasspath());
        }

        List<String> commands = new ArrayList<String>();
        if (javaCmd.indexOf('|') >= 0) {
            // Pipe char can be used to tokenize command
            commands.addAll(Arrays.asList(javaCmd.split("\\|")));
        }
        else {
            commands.add(javaCmd);
        }
        commands.addAll(slaveArgs);

        if (config.getAgentEntries() != null) {
            for (AgentEntry agentEntry : config.getAgentEntries()) {
                final String agentPath = console.cacheFile(agentEntry);
                final String options = agentEntry.getOptions();
                commands.add("-javaagent:" + agentPath + (options == null ? "" : "=" + options));
            }
        }

        if (bootstraper != null) {
            commands.add("-jar");
            commands.add(bootstraper);
        }
        else {
            commands.add("-cp");
            commands.add(shallowClasspath);
            commands.add(bootstartClass());
        }

        slaveWD = isEmpty(slaveWD) ? "." : slaveWD;

        session.lifecycleListener = config.getLifecycleListener();
        session.launchConf = new LaunchConf(
                config.getNodeName(),
                console.getHostname(),
                slaveWD,
                commands,
                slaveEnv);


        console.startProcess(slaveWD, commands.toArray(new String[0]), slaveEnv, session);

        return session;
    }

    private boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    private String makeCpLine(List<String> cp) {
        String psep = System.getProperty("path.separator");
        StringBuilder sb = new StringBuilder();
        for(String cpe: cp) {
            if (sb.length() > 0) {
                sb.append(psep);
            }
            sb.append(cpe);
        }
        return sb.toString();
    }

    private String buildBootJar(HostControlConsole console, List<ClasspathEntry> jvmClasspath) {

        List<String> paths = console.cacheFiles(jvmClasspath);

        StringBuilder remoteClasspath = new StringBuilder();
        for(String path: paths) {
            if (remoteClasspath.length() > 0) {
                remoteClasspath.append(' ');
            }
            remoteClasspath.append(convertToURI(path));
        }

        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.CLASS_PATH, remoteClasspath.toString());
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, bootstartClass());

        byte[] booter;
        try {
            booter = ClasspathUtils.createManifestJar(mf);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        FileBlob bb = Classpath.createBinaryEntry("booter.jar", booter);

        String path = console.cacheFile(bb);

        return path;
    }

    private String bootstartClass() {
        return SmartBootstraper.class.getName();
    }

    private Object convertToURI(String path) {
        if (path.indexOf(' ') >= 0 || path.indexOf(':') >= 0 || path.indexOf('\\') >= 0) {
            StringBuilder sb = new StringBuilder();
            if (path.charAt(1) == ':') {
                sb.append("file:/");
            }
            for(int i = 0; i != path.length(); ++i) {
                // TODO proper URL escaping
                char ch = path.charAt(i);
                if (ch == '\\') {
                    sb.append('/');
                }
                else if (ch == ' ') {
                    sb.append("%20");
                }
                else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        else {
            return path;
        }
    }

    private byte[] serialize(Object obj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            byte[] binspore = bos.toByteArray();
            return binspore;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static <T> T tryFget(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            return null;
        }
    }

    static <T> T fget(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException)e.getCause();
            }
            else if (e.getCause() instanceof Error) {
                throw (Error)e.getCause();
            }
            else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    static <T> T uget(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            // ignore
            return null;
        }
    }

    private static class CallbackSporePlanter implements Runnable, Serializable {

        private static final long serialVersionUID = 20130928L;

        SlaveSpore spore;
        String masterHost;
        int masterPort;

        public CallbackSporePlanter(SlaveSpore spore, String masterHost, int masterPort) {
            this.spore = spore;
            this.masterHost = masterHost;
            this.masterPort = masterPort;
        }

        @Override
        public void run() {
            spore.start(new ConnectSocketConnector(new InetSocketAddress(masterHost, masterPort)));
        }

        @Override
        public String toString() {
            return spore + " + call home [" + masterHost + ":" + masterPort + "]";
        }
    }

    private static class ConnectSocketConnector implements DuplexStreamConnector, Serializable {

        private static final long serialVersionUID = 20131217L;

        private final SocketAddress address;

        public ConnectSocketConnector(SocketAddress address) {
            this.address = address;
        }

        @Override
        public DuplexStream connect() throws IOException {
            Socket socket = new Socket();
            socket.connect(address);

            return new SocketStream(socket);
        }

        @Override
        public String toString() {
            return String.valueOf(address);
        }
    }

    private static class ProcessStreams {

        OutputStream stdIn;
        LookbackOutputStream stdOut;
        Link eofOut;
        LookbackOutputStream stdErr;
        Link eofErr;

    }

    // TODO shutdown sequence is still fishy
    private static class ControlledSession implements ManagedProcess, ProcessHandler, SocketHandler {

        RemoteExecutionSession session;
        FutureBox<SocketAddress> bindAddress = new FutureBox<SocketAddress>();
        byte[] binspore;
        FutureBox<ProcessStreams> procStreams = new FutureBox<ProcessStreams>();
        FutureBox<Integer> exitCode = new FutureBox<Integer>();
        FutureBox<AdvancedExecutor> executor = new FutureBox<AdvancedExecutor>();
        Destroyable socketHandle;
        volatile Destroyable procHandle;
        StreamCopyService streamCopyService;
        ProcessLifecycleListener lifecycleListener;
        LaunchConf launchConf;
        /** Process has been started so we except exit code to be invoked eventually */
        boolean procStarted;

        @Override
        public void bound(String host, int port) {
            bindAddress.setData(InetSocketAddress.createUnresolved(host, port));
        }

        @Override
        public void accepted(String remoteHost, int remotePort, InputStream soIn, OutputStream soOut) {
            // TODO logging
            session.setTransportConnection(new NamedStreamPair("tunnel(" + remoteHost + ":" + remotePort + ")", soIn, soOut));
            executor.setData(session.getRemoteExecutor());
        }

        @Override
        public void terminated(String message) {
            if (!executor.isDone()) {
                sepuku(new IOException("Transport terminated: " + message));
            }
        }

        @Override
        public void started(OutputStream stdIn, InputStream stdOut, InputStream stdErr) {
            procStarted = true;
            ProcessStreams ps = new ProcessStreams();
            ps.stdIn = stdIn;
            ps.stdOut = new LookbackOutputStream(16 << 10);
            ps.stdErr = new LookbackOutputStream(16 << 10);
            ps.eofOut = streamCopyService.link(stdOut, ps.stdOut);
            ps.eofErr = streamCopyService.link(stdErr, ps.stdErr);

            fireStarted();

            try {
                // TO DO just for debug
//				ps.stdOut.setOutput(System.out);
//				ps.stdErr.setOutput(System.err);

                DataOutputStream dos = new DataOutputStream(stdIn);
                dos.writeInt(binspore.length);
                dos.write(binspore);
                dos.flush();
            } catch (IOException e) {
                // need to set stream to allow read console errors
                procStreams.setData(ps);
                sepuku(e);
                return;
            }
            procStreams.setData(ps);
        }

        @Override
        public void execFailed(OutputStream stdIn, InputStream stdOut, InputStream stdErr, String error) {
            procStarted = true;
            ProcessStreams ps = new ProcessStreams();
            ps.stdIn = stdIn;
            ps.stdOut = new LookbackOutputStream(16 << 10);
            ps.stdErr = new LookbackOutputStream(16 << 10);
            ps.eofOut = streamCopyService.link(stdOut, ps.stdOut);
            ps.eofErr = streamCopyService.link(stdErr, ps.stdErr);

            procStreams.setData(ps);

            fireExecFailed(error);

            flushAndDie(Integer.MIN_VALUE, "Exec failed: " + error);
        }

        @Override
        public void finished(int exitCode) {
            fireTerminated(exitCode);
            flushAndDie(exitCode, "Terminated, exitCode=" + exitCode);
        }

        private void fireStarted() {
            if (lifecycleListener != null) {
                try {
                    lifecycleListener.processStarted(launchConf.nodeName, launchConf);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        private void fireExecFailed(String error) {
            if (lifecycleListener != null) {
                launchConf.error = error;
                try {
                    lifecycleListener.processExecFailed(launchConf.nodeName, launchConf);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        private void fireTerminated(int exitCode) {
            if (lifecycleListener != null) {
                TermInfo ti = new TermInfo(launchConf.nodeName, launchConf.hostname, exitCode);
                try {
                    lifecycleListener.processTerminated(launchConf.nodeName, ti);
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        private void flushAndDie(int exitCode, String error) {
            try {
                this.exitCode.setData(exitCode);
            }
            catch(IllegalStateException e) {
                // ignore
            }
            ProcessStreams ps = fget(procStreams);
            if (ps != null) {
                try {
                    ps.eofOut.flushAndClose();
                    ps.eofErr.flushAndClose();
                    if (ps.stdOut.getOutput() == null) {
                        byte[] bb = ps.stdOut.getLookbackBuffer();
                        if (bb.length > 0) {
                            System.out.println(new String(bb));
                        }
                    }
                    if (ps.stdErr.getOutput() == null) {
                        byte[] bb = ps.stdErr.getLookbackBuffer();
                        if (bb.length > 0) {
                            System.err.println(new String(bb));
                        }
                    }
                }
                catch(Exception e) {
                    // ignore
                }
            }
            sepuku(new RuntimeException(error));
        }

        @Override
        public AdvancedExecutor getExecutionService() {
            return fget(executor);
        }

        @Override
        public void bindStdIn(InputStream is) {
            ProcessStreams ps = fget(procStreams);
            if (is != null) {
                streamCopyService.link(is, ps.stdIn);
            }
            else {
                try {
                    ps.stdIn.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void bindStdOut(OutputStream os) {
            ProcessStreams ps = fget(procStreams);
            try {
                if (os != null) {
                    ps.stdOut.setOutput(os);
                }
                else {
                    ps.stdOut.close();
                }
            } catch (IOException e) {
                sepuku(e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void bindStdErr(OutputStream os) {
            ProcessStreams ps = fget(procStreams);
            try {
                if (os != null) {
                    ps.stdErr.setOutput(os);
                }
                else {
                    ps.stdErr.close();
                }
            } catch (IOException e) {
                sepuku(e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public void suspend() {
            throw new UnsupportedOperationException();

        }

        @Override
        public void resume() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void consoleFlush() {
            if (procStreams.isDone()) {
                ProcessStreams ps = fget(procStreams);
                ps.eofOut.flush();
                ps.eofErr.flush();
            }
        }

        @Override
        public void destroy() {
            sepuku(new RuntimeException("Terminated"));
        }

        protected synchronized void sepuku(Throwable e) {
            session.terminate(e);
            procStreams.setErrorIfWaiting(e);
            executor.setErrorIfWaiting(e);
            // do not add exception to exit code as it may be used even in case of abnormal termination
            if (!procStarted) {
                exitCode.setErrorIfWaiting(e);
            }
            if (procHandle != null) {
                finalConsoleFlush();
                procHandle.destroy();
            }
            if (socketHandle != null) {
                socketHandle.destroy();
            }
        }

        private void finalConsoleFlush() {
            try {
                consoleFlush();
            }
            catch(Exception x) {
                // ignore
            }
        }

        @Override
        public FutureEx<Integer> getExitCodeFuture() {
            return exitCode;
        }
    }

    private static class LaunchConf implements ExecFailedInfo {

        private final String nodeName;
        private final String hostname;
        private final String workPath;
        private final List<String> command;
        private final Map<String, String> addedEnv;
        String error;

        public LaunchConf(String nodeName, String hostname, String workPath, List<String> command,
                Map<String, String> addedEnv) {
            super();
            this.nodeName = nodeName;
            this.hostname = hostname;
            this.workPath = workPath;
            this.command = command;
            this.addedEnv = addedEnv;
        }

        @Override
        public String getNodeName() {
            return nodeName;
        }

        @Override
        public String getHostname() {
            return hostname;
        }

        @Override
        public String getWorkPath() {
            return workPath;
        }

        @Override
        public List<String> getCommand() {
            return command;
        }

        @Override
        public Map<String, String> getAddedEnvironment() {
            return addedEnv;
        }

        @Override
        public String getError() {
            return error;
        }
    }

    private static class TermInfo implements TerminationInfo {

        private final String nodename;
        private final String hostname;
        private final int exitCode;

        public TermInfo(String nodename, String hostname, int exitCode) {
            this.nodename = nodename;
            this.hostname = hostname;
            this.exitCode = exitCode;
        }

        @Override
        public String getNodeName() {
            return nodename;
        }

        @Override
        public String getHostname() {
            return hostname;
        }

        @Override
        public int getExitCode() {
            return exitCode;
        }
    }
}
