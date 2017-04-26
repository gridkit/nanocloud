package org.gridkit.vicluster.telecontrol.ssh;

import static org.gridkit.nanocloud.RemoteNode.REMOTE;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.RemoteNode;
import org.gridkit.nanocloud.SimpleCloudFactory;
import org.gridkit.nanocloud.VX;
import org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LinuxSocketCheck {

	private Cloud cloud;
	
	@Before
	public void initCloud() {
		cloud = SimpleCloudFactory.createSimpleSshCloud();
	}

	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void sudo_check() {
	    ViNode node = cloud.node("cbox1");
	    
	    node.x(REMOTE).setRemoteBootstrapJavaExec("sudo java");
	    
	    node.exec(new Callable<Void>(){

            @Override
            public Void call() throws Exception {
                System.out.println("ping");
                return null;
            }	        
	    });
	}
	
	@Test
	public void simple_socket_transfer() throws IOException {

		cloud.node("cbox1").exec(new Callable<Void>(){

			@Override
			public Void call() throws Exception {
				ServerSocket ss = new ServerSocket();
				ss.setReuseAddress(true);
				ss.bind(null);
				Socket s1 = new Socket();
				s1.connect(ss.getLocalSocketAddress());
				Socket s2 = ss.accept();

				DataOutputStream dos = new DataOutputStream(s1.getOutputStream());
				DataInputStream dis = new DataInputStream(s2.getInputStream());
				
				dos.writeUTF("Hallo world!");
				String result = dis.readUTF();
				
				Assert.assertEquals("Hallo world!", result);
				return null;
			}
		});
	}

	@Test
	public void server_socket_close() throws IOException {

		cloud.node("cbox1").exec(new Callable<Void>() {
			
			@Override
			public Void call() throws Exception {
				ServerSocket ss = new ServerSocket();
				ss.setReuseAddress(true);
				ss.bind(null);
				Socket s1 = new Socket();
				s1.connect(ss.getLocalSocketAddress());
				Socket s2 = ss.accept();
			
				ss.close();
				
				DataOutputStream dos = new DataOutputStream(s1.getOutputStream());
				DataInputStream dis = new DataInputStream(s2.getInputStream());
				
				dos.writeUTF("Hallo world!");
				String result = dis.readUTF();
				
				Assert.assertEquals("Hallo world!", result);
				return null;
			}
		});
	}
}
