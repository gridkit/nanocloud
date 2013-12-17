package org.gridkit.nanocloud.test.maven;

import static org.gridkit.nanocloud.test.maven.MavenClasspathConfig.MAVEN;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.util.concurrent.RecuringTask;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.junit.Assert;
import org.junit.Test;

public class MavenClasspathManagerTest {

	@Test
	public void verify_self_version() {
		String ver = MavenClasspathManager.getArtifactVersion("org.gridkit.lab", "nanotest");
		System.out.println("verify_self_version: " + ver);
		Assert.assertTrue(ver != null);
	}

	@Test
	public void verify_junit_version() {
		String ver = MavenClasspathManager.getArtifactVersion("junit", "junit");
		System.out.println("verify_junit_version: " + ver);
		Assert.assertTrue(ver != null);
	}

	@Test
	public void verify_local_repo_detection() {
		File path = MavenClasspathManager.getLocalMavenRepoPath();
		System.out.println("verify_local_repo_detection: " + path);
		Assert.assertTrue(path != null);
	}

	@Test
	public void verify_find_versions() {
		List<String> versions = MavenClasspathManager.getAvailableVersions("org.gridkit.lab", "viconcurrent");
		System.out.println("verify_find_versions: " + versions);
		Assert.assertTrue(!versions.isEmpty());
	}
	
	@Test
	public void verify_find_junit_jar() {
		// maven meta data is not available in this jar
		URL path = MavenClasspathManager.findJar("junit", "junit", MavenClasspathManager.getArtifactVersion("junit", "junit"));
		URL cppath = MavenClasspathManager.getArtifactClasspathUrl("junit", "junit");
		System.out.println("verify_find_junit_jar [jar path]: " + path);
		System.out.println("verify_find_junit_jar [base url]: " + cppath);
		Assert.assertTrue(path != null);
		Assert.assertTrue(cppath != null);
	}

	@Test
	public void verify_find_slf4j_jar() {
		// maven meta data is present in this jar
		String group = "org.slf4j";
		String artifact = "slf4j-api";
		URL path = MavenClasspathManager.findJar(group, artifact, MavenClasspathManager.getArtifactVersion(group, artifact));
		URL cppath = MavenClasspathManager.getArtifactClasspathUrl(group, artifact);
		System.out.println("verify_find_slf4j_jar [jar path]: " + path);
		System.out.println("verify_find_slf4j_jar [base url]: " + cppath);
		Assert.assertTrue(path != null);
		Assert.assertTrue(cppath != null);
	}
	
	@Test(expected = NoSuchMethodError.class)
	public void verify_version_replacement__no_such_method() {
		Cloud cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setIsolateType();
		try {
			System.out.println("viconcurrent URL: " + MavenClasspathManager.getArtifactClasspathUrl("org.gridkit.lab", "viconcurrent"));				
			ViNode node =cloud.node("viconcurrent-0.7.15");
			MavenClasspathManager.replaceArtifactVersion(node, "org.gridkit.lab", "viconcurrent", "0.7.15");
			node.exec(new Runnable() {
				
				@Override
				public void run() {
					System.out.println("viconcurrent version: " + MavenClasspathManager.getArtifactVersion("org.gridkit.lab", "viconcurrent"));				
					// this should throw NoSuchMethodError, because this method was misspelled in 0.7.15
					RecuringTask task = new RecuringTask(null, null, 0, TimeUnit.SECONDS);
					task.canceled();					
				}
			});
		}
		finally {
			cloud.shutdown();
		}
	}	

	@Test(expected = NoSuchMethodError.class)
	public void verify_version_replacement__no_such_method__out_of_proc() {
		Cloud cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setLocalType();
		try {
			System.out.println("viconcurrent URL: " + MavenClasspathManager.getArtifactClasspathUrl("org.gridkit.lab", "viconcurrent"));				
			ViNode node =cloud.node("viconcurrent-0.7.15");
			MavenClasspathManager.replaceArtifactVersion(node, "org.gridkit.lab", "viconcurrent", "0.7.15");
			node.exec(new Runnable() {
				
				@Override
				public void run() {
					System.out.println("viconcurrent version: " + MavenClasspathManager.getArtifactVersion("org.gridkit.lab", "viconcurrent"));				
					// this should throw NoSuchMethodError, because this method was misspelled in 0.7.15
					RecuringTask task = new RecuringTask(null, null, 0, TimeUnit.SECONDS);
					task.canceled();					
				}
			});
		}
		finally {
			cloud.shutdown();
		}
	}	

	@Test
	public void verify_version_replacement__meta_data() {
		Cloud cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setIsolateType();
		try {
			System.out.println("viconcurrent URL: " + MavenClasspathManager.getArtifactClasspathUrl("org.gridkit.lab", "viconcurrent"));				
			ViNode node =cloud.node("viconcurrent-0.7.15");
			MavenClasspathManager.replaceArtifactVersion(node, "org.gridkit.lab", "viconcurrent", "0.7.15");
			String version = node.exec(new Callable<String>() {
				
				@Override
				public String call() throws Exception {
					String cppath = "/META-INF/maven/org.gridkit.lab/viconcurrent/pom.properties";
					InputStream is = getClass().getResourceAsStream(cppath);
					Properties prop = new Properties();
					prop.load(is);
					is.close();
					return prop.getProperty("version");
				}
			});
			
			System.out.println("viconcurrent version: " + version);
			Assert.assertEquals("0.7.15", version);
		}
		finally {
			cloud.shutdown();
		}
	}	

	@Test
	public void verify_version_replacement__meta_data__out_of_proc() {
		Cloud cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setLocalType();
		try {
			System.out.println("viconcurrent URL: " + MavenClasspathManager.getArtifactClasspathUrl("org.gridkit.lab", "viconcurrent"));				
			ViNode node = cloud.node("viconcurrent-0.7.15");
			node.x(MAVEN).replace("org.gridkit.lab", "viconcurrent", "0.7.15");
			String version = node.exec(new Callable<String>() {
				
				@Override
				public String call() throws Exception {
					String cppath = "/META-INF/maven/org.gridkit.lab/viconcurrent/pom.properties";
					InputStream is = getClass().getResourceAsStream(cppath);
					Properties prop = new Properties();
					prop.load(is);
					is.close();
					return prop.getProperty("version");
				}
			});
			
			System.out.println("viconcurrent version: " + version);
			Assert.assertEquals("0.7.15", version);
		}
		finally {
			cloud.shutdown();
		}
	}	
}
