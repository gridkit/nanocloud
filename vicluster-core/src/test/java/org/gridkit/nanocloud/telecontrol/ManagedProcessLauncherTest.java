package org.gridkit.nanocloud.telecontrol;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.telecontrol.ProcessLauncher.LaunchConfig;
import org.gridkit.nanocloud.viengine.ProcessLifecycleListener;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ManagedProcessLauncherTest {

    static {
        System.setProperty("org.gridkit.suppress-system-exit", "true");
        System.setProperty(ZLogFactory.PROP_ZLOG_MODE, "slf4j");
    }

    private LocalControlConsole console;
    private RemoteExecutionSession session;

    @Before
    public void initConsole() {
        console = new LocalControlConsole();
        session = new ZeroRmiRemoteSession("test");
    }

    @After
    public void destroyConsole() {
        console.terminate();
        session.terminate(null);
    }

    @Test(timeout = 10000)
    public void startSlave() throws InterruptedException, ExecutionException {

        LaunchConfig conf = new LaunchConfig() {

            @Override
            public String getNodeName() {
                return "test";
            }

            @Override
            public HostControlConsole getControlConsole() {
                return console;
            }

            @Override
            public RemoteExecutionSession getRemotingSession() {
                return session;
            }

            @Override
            public List<ClasspathEntry> getSlaveClasspath() {
                return Classpath.getClasspath(Thread.currentThread().getContextClassLoader());
            }

            @Override
            public String getSlaveWorkDir() {
                return null;
            }

            @Override
            public List<String> getSlaveShallowClasspath() {
                return null;
            }

            @Override
            public String getSlaveJvmExecCmd() {
                return new File(new File(System.getProperty("java.home"), "bin"), "java").getPath();
            }

            @Override
            public Map<String, String> getSlaveEnv() {
                return null;
            }

            @Override
            public List<String> getSlaveArgs() {
                return new ArrayList<String>();
            }

            @Override
            public ProcessLifecycleListener getLifecycleListener() {
                return null;
            }

            @Override
            public List<AgentEntry> getAgentEntries() {
                return null;
            }

            @Override
            public StreamCopyService getStreamCopyService() {
                return BackgroundStreamDumper.SINGLETON;
            }
        };

        ProcessSporeLauncher launcher = new ProcessSporeLauncher(null);
        ManagedProcess slave = launcher.launchProcess(conf);

        slave.bindStdOut(System.out);
        slave.bindStdErr(System.err);

        slave.bindStdIn(new ByteArrayInputStream("Ping".getBytes()));

        slave.getExecutionService().submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                Assert.assertEquals("Ping", br.readLine());
                Assert.assertNull(br.readLine());
                System.out.println("Ping received");
                return null;
            }
        }).get();

        slave.destroy();

        Assert.assertEquals((Integer)0, slave.getExitCodeFuture().get());

    }
}
