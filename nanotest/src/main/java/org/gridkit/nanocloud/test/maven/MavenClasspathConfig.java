package org.gridkit.nanocloud.test.maven;

import com.tobedevoured.naether.api.Naether;
import com.tobedevoured.naether.impl.NaetherImpl;
import org.apache.maven.model.Dependency;
import org.gridkit.nanocloud.VX;
import org.gridkit.vicluster.ViConfigurable;
import org.gridkit.vicluster.ViConfExtender;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;

import java.net.URL;
import java.util.Set;

public class MavenClasspathConfig extends ViConfigurable.Delegate {

	public static final ViConfExtender<MavenClasspathConfig> MAVEN = new ViConfExtender<MavenClasspathConfig>() {
		public MavenClasspathConfig wrap(ViConfigurable node) {
			return new MavenClasspathConfig(node);
		}
	};
	
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

    public MavenClasspathConfig addWithTransitive(String groupId, String artifactId, String version) throws Exception {
        return addWithTransitive(groupId, artifactId, version, null);
    }

    public MavenClasspathConfig addWithTransitive(String groupId, String artifactId, String version, Set<RemoteRepository> remoteRepositories) throws Exception {
        Naether naether = new NaetherImpl();
        if (remoteRepositories != null) {
            naether.setRemoteRepositories(remoteRepositories);
        }
        final Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        naether.addDependency(dependency);
        naether.resolveDependencies();
        for (org.sonatype.aether.graph.Dependency resolvedDependency : naether.currentDependencies()) {
            final Artifact artifact = resolvedDependency.getArtifact();
            add(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        }
        return this;
    }
}
