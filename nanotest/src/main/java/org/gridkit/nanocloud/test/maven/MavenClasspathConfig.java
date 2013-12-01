package org.gridkit.nanocloud.test.maven;

import org.gridkit.vicluster.ViConfigurable;;

public class MavenClasspathConfig extends ViConfigurable.Delegate {

	private ViConfigurable delegate;
	
	public MavenClasspathConfig(ViConfigurable delegate) {
		this.delegate = delegate;
	}

	@Override
	protected ViConfigurable getConfigurable() {
		return delegate;
	}
	
	public MavenClasspathConfig add(String groupId, String artifactId, String version) {
		MavenClasspathManager.addArtifactVersion(getConfigurable(), groupId, artifactId, version);
		return this;
	}

	public MavenClasspathConfig replace(String groupId, String artifactId, String version) {
		MavenClasspathManager.addArtifactVersion(getConfigurable(), groupId, artifactId, version);
		try {
			MavenClasspathManager.removeArtifactVersion(getConfigurable(), groupId, artifactId);
		}
		catch(IllegalArgumentException x) {
			// ignore
		}
		return this;
	}

	public MavenClasspathConfig remove(String groupId, String artifactId, String version) {
		MavenClasspathManager.removeArtifactVersion(getConfigurable(), groupId, artifactId, version);
		return this;
	}

	public MavenClasspathConfig remove(String groupId, String artifactId) {
		MavenClasspathManager.removeArtifactVersion(getConfigurable(), groupId, artifactId);
		return this;
	}
}
