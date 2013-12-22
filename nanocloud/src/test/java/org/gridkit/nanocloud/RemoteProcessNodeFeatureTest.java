package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.RemoteNode.REMOTE;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

public class RemoteProcessNodeFeatureTest extends ProcessNodeFeatureTest {

	@BeforeClass
	public static void check_cbox1() {
		Cloud c = CloudFactory.createCloud();
		try {
			c.node("**").x(REMOTE)
				.useSimpleRemoting()
				.setRemoteHost("cbox1");
			
			c.node("test").touch();
			c.shutdown();
		}
		catch(Exception e) {
			e.printStackTrace();
			Assume.assumeTrue(false);
		}
		finally {
			c.shutdown();
		}
	}
	
	@Before
	@Override
	public void initCloud() {
		cloud = CloudFactory.createCloud();
		cloud.node("**").x(REMOTE)
			.useSimpleRemoting()
			.setRemoteHost("cbox1");
	}

	
	
}
