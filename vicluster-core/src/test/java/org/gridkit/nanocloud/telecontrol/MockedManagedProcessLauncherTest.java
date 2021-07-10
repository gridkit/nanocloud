package org.gridkit.nanocloud.telecontrol;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import org.gridkit.vicluster.telecontrol.StreamPipe;
import org.gridkit.vicluster.telecontrol.bootstraper.SmartBootstraper;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MockedManagedProcessLauncherTest {

    static {
        System.setProperty("org.gridkit.suppress-system-exit", "true");
        System.setProperty(ZLogFactory.PROP_ZLOG_MODE, "slf4j");
    }

    private LocalControlConsole console;
    private RemoteExecutionSession session;

    @Before
    public void initConsole() {
        console = new LocalControlConsole() {

            @Override
            public Destroyable startProcess(String workingDir, String[] command, Map<String, String> environment, final ProcessHandler handler) {
                final StreamPipe pipeIn = new StreamPipe(4096);
                StreamPipe pipeOut = new StreamPipe(4096);

                handler.started(pipeIn.getOutputStream(), pipeOut.getInputStream(), pipeOut.getInputStream());

                Thread slave = new Thread() {
                    @Override
                    public void run() {
                        int exitCode = 1;
                        try {
                            SmartBootstraper.start(pipeIn.getInputStream());
                            exitCode = 0;
                        }
                        finally {
                            handler.finished(exitCode);
                        }
                    }
                };

                slave.setName("slave");
                slave.start();

                return null;
            }

        };
        session = new ZeroRmiRemoteSession("test");
    }

    @After
    public void destroyConsole() {
        console.terminate();
        session.terminate(null);
    }

    @Test
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

        ProcessSporeLauncher launcher = new ProcessSporeLauncher(BackgroundStreamDumper.SINGLETON);
        ManagedProcess slave = launcher.launchProcess(conf);

        slave.bindStdOut(System.out);
        slave.bindStdErr(System.err);

        slave.bindStdIn(new ByteArrayInputStream("Ping".getBytes()));

        slave.getExecutionService().submit(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
//				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//				Assert.assertEquals("Ping", br.readLine());
//				Assert.assertNull(br.readLine());
                System.out.println("Ping check skipped");
                return null;
            }
        }).get();

        try {
            slave.destroy();
        } catch (Exception e) {
            // ignore
        }

        Assert.assertEquals((Integer)0, slave.getExitCodeFuture().get());

    }
}
