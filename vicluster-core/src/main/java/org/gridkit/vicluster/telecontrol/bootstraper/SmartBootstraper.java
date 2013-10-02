package org.gridkit.vicluster.telecontrol.bootstraper;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class SmartBootstraper {

	public SmartBootstraper() {
	}

	public static void main(String[] args) throws IOException {
		
		Runnable spore;
		try {
			DataInputStream di = new DataInputStream(System.in);
			int blobSize = di.readInt();
			byte[] blob = new byte[blobSize];
			di.readFully(blob);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(blob));
			spore = (Runnable) ois.readObject();
		}
		catch(Throwable e) {
			System.err.println("Failed to read spore");
			e.printStackTrace();
			System.exit(1);
			return;
		}
		try {
			spore.run();
		}
		catch(Throwable e) {
			System.err.println("Failed to execute spore");
			System.err.println("Spore: " + spore);
			e.printStackTrace();
			System.exit(1);
			return;
		}
		
		if (System.getProperty("org.gridkit.suppress-system-exit") == null) {
			System.exit(0);
			return;
		}
	}	
}
