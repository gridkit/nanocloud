package org.gridkit.vicluster.telecontrol.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.TaskService;

public class StreamDumperTask implements TaskService.Task {

	private final TaskService service;
	private final InputStream is;
	private final OutputStream os;
	private final int sleepDelay;

	public StreamDumperTask(TaskService service, InputStream is, OutputStream os) {
		this(service, is, os, 50);
	}

	public StreamDumperTask(TaskService service, InputStream is, OutputStream os, int sleepDelay) {
		this.service = service;
		this.is = is;
		this.os = os;
		this.sleepDelay = sleepDelay;
	}

	@Override
	public void run() {
		try {
			if (is.available() > 0) {
				int size = is.available();
				if (size > 16 << 10) {
					size = 16 << 10;
				}
				byte[] buf = new byte[size];
				int n = is.read(buf);
				os.write(buf, 0, n);
				
				service.schedule(this);
			}
			else {
				service.schedule(this, sleepDelay, TimeUnit.MILLISECONDS);
			}
		}
		catch(IOException e) {
			// TODO report unexpected exceptions
			// just die
		}
	}

	@Override
	public void interrupt(Thread taskThread) {
		try {
			is.close();
		}
		catch(IOException e) {
			// ignore;
		}
		try {
			os.write("\nCANCELED\n".getBytes());
			os.close();
		}
		catch(IOException e) {
			// ignore;
		}
	}

	@Override
	public void cancled() {
		try {
			os.write("\nCANCELED\n".getBytes());
		} catch (IOException e) {
			// ignore
		}
	}
}
