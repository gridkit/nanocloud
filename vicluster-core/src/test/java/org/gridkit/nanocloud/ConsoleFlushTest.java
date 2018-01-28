package org.gridkit.nanocloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;

public class ConsoleFlushTest {

	@Rule
	public Retry retry = new Retry(3);
	
	@Test
	public void verify_output_redirect_no_flushing() {
		
		Cloud c = CloudFactory.createCloud();
		c.node("**").x(VX.TYPE).setLocal();
		
		StringWriter writer = new StringWriter();
		c.node("test").x(VX.CONSOLE).bindOut(writer);
		
		c.node("test").exec(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("Ping");
			}
		});
		
		// due to asynchronous work of stream, "Ping" is not visible yet
		assertThat(writer.toString()).isEmpty();		
	}

	@Test
	public void verify_output_redirect_with_flush() {
		
		Cloud c = CloudFactory.createCloud();
		c.node("**").x(VX.TYPE).setLocal();
		
		StringWriter writer = new StringWriter();
		c.node("test").x(VX.CONSOLE).bindOut(writer);
		
		c.node("test").exec(new Runnable() {
			
			@Override
			public void run() {
				System.out.print("Ping");
			}
		});
		
		c.node("test").x(VX.CONSOLE).flush();
		
		// due to asynchronous work of stream, "Ping" is not visible yet
		assertThat(writer.toString()).isEqualTo("Ping");		
	}	
}
