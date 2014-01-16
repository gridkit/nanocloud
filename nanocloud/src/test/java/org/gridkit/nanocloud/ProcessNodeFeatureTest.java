package org.gridkit.nanocloud;

import static org.gridkit.vicluster.VX.TYPE;
import static org.gridkit.vicluster.VX.PROCESS;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.ViNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProcessNodeFeatureTest {

	protected Cloud cloud;
	
	@Before
	public void initCloud() {
		cloud = CloudFactory.createCloud();
		cloud.node("**").x(TYPE).setLocal();
	}

	@After
	public void shutdownCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void verify_new_env_variable() {
		
		ViNode node = cloud.node("verify_new_env_variable");
		node.x(PROCESS).setEnv("TEST_VAR", "TEST");
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("TEST",System.getenv("TEST_VAR"));
			}
		});
	}

	@Test
	public void verify_env_variable_removal() {
		
		ViNode node = cloud.node("verify_env_variable_removal");
		node.x(PROCESS).setEnv("HOME", null);
		node.x(PROCESS).setEnv("HOMEPATH", null);
		node.exec(new Runnable() {
			@Override
			public void run() {				
				Assert.assertFalse("HOME expected to be empty", System.getenv().containsKey("HOME"));
				Assert.assertFalse("HOMEPATH expected to be empty", System.getenv().containsKey("HOMEPATH"));
			}
		});
	}
	
	@Test
	public void verify_jvm_single_arg_passing() {
		
		ViNode node = cloud.node("verify_jvm_single_arg_passing");
		node.x(PROCESS).addJvmArg("-DtestProp=TEST");
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("TEST",System.getProperty("testProp"));
			}
		});
	}

	@Test
	public void verify_jvm_multiple_arg_passing() {
		
		ViNode node = cloud.node("verify_jvm_multiple_arg_passing");
		node.x(PROCESS).addJvmArg("-DtestProp=TEST");
		node.x(PROCESS).addJvmArgs("-DtestProp1=A", "-DtestProp2=B");
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("TEST",System.getProperty("testProp"));
				Assert.assertEquals("A",System.getProperty("testProp1"));
				Assert.assertEquals("B",System.getProperty("testProp2"));
			}
		});
	}

	@Test
	public void verify_slave_working_dir() throws IOException {
		
		ViNode nodeB = cloud.node("verify_slave_working_dir.base");
		ViNode nodeC = cloud.node("verify_slave_working_dir.child");
		nodeC.x(PROCESS).setWorkDir("target");
		cloud.node("**").touch();
		final File base = nodeB.exec(new Callable<File>() {
			@Override
			public File call() throws Exception {
				File wd = new File(".").getCanonicalFile();
				new File(wd, "target").mkdirs();
				return wd;
			}
		});
		nodeC.exec(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				File wd = new File(".").getCanonicalFile();
				Assert.assertEquals(new File(base, "target").getCanonicalFile(), wd);
				return null;
			}
		});
	}
}
