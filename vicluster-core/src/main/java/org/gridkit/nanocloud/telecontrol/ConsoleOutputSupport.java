package org.gridkit.nanocloud.telecontrol;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViEngine.PragmaHandler;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.ViEngine.WritableSpiConfig;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.ViSpiConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public class ConsoleOutputSupport implements PragmaHandler {

	private static String MUX_STREAM = "#spi-console:mux-stream.";
	private static String SILENCER_HOOK = "hook:console-silencer";
	private static String FINAL_FLUSH_HOOK = "hook:final-console-flush";
	
	@Override
	public Object get(String key, ViEngine engine) {
		return engine.getConfig().get(key);
	}

	@Override
	public synchronized void set(String key, Object value, ViEngine engine, WritableSpiConfig wc) {
		if (ViConf.CONSOLE_FLUSH.equals(key)) {
			flushConsole(engine.getConfig());
		}
		else if (ViConf.CONSOLE_SILENT_SHUTDOWN.equals(key)) {			
			setSilentShutdown(engine, wc, toBoolean(value));
		}
		else if (ViConf.CONSOLE_STD_IN.equals(key)) {
			setInputPipe(engine, wc, (InputStream) value);
		}
		else if (ViConf.CONSOLE_STD_OUT.equals(key)) {
			setOutputPipe(engine, wc, "out", (OutputStream) value);
		}
		else if (ViConf.CONSOLE_STD_ERR.equals(key)) {
			setOutputPipe(engine, wc, "err", (OutputStream) value);
		}
		else if (ViConf.CONSOLE_STD_OUT_ECHO.equals(key)) {
			setOutputPipeEcho(engine, wc, "out", toBoolean(value));
		}
		else if (ViConf.CONSOLE_STD_ERR_ECHO.equals(key)) {
			setOutputPipeEcho(engine, wc, "err", toBoolean(value));
		}
		else if (ViConf.CONSOLE_STD_OUT_ECHO_STREAM.equals(key)) {
			setOutputPipeEchoStream(engine, wc, "out", new PrintStream((OutputStream) value));
		}
		else if (ViConf.CONSOLE_STD_ERR_ECHO_STREAM.equals(key)) {
			setOutputPipeEchoStream(engine, wc, "err", new PrintStream((OutputStream) value));
		}
		else if (ViConf.CONSOLE_ECHO_PREFIX.equals(key)) {
			setEchoPrefix(engine, wc, "out", (String)value);
			setEchoPrefix(engine, wc, "err", (String)value);
		}
		else {
			throw new IllegalArgumentException("Unknown pragma: " + key);
		}
	}

	private void setSilentShutdown(ViEngine engine, WritableSpiConfig wc, boolean enabled) {
		wc.setProp(SILENCER_HOOK, enabled ? new Silencer() : null);
		wc.setProp(FINAL_FLUSH_HOOK, enabled ? null : new FinalFlush());
	}

	private void setInputPipe(ViEngine engine,	WritableSpiConfig wc, InputStream is) {
		ManagedProcess mp = engine.getConfig().getManagedProcess();
		check(mp);
		mp.bindStdIn(is);
	}

	private void setOutputPipe(ViEngine engine, WritableSpiConfig wc, String stream, OutputStream out) {
		ConsoleMultiplexorStream cms = ensureStreamMux(engine, wc, stream);
		cms.outs[1] = out;
	}

	private void setOutputPipeEcho(ViEngine engine, WritableSpiConfig wc, String stream, boolean enabled) {
		ConsoleMultiplexorStream cms = ensureStreamMux(engine, wc, stream);
		((WrapperPrintStream)cms.outs[0]).silence = !enabled;
	}

	private void setOutputPipeEchoStream(ViEngine engine, WritableSpiConfig wc, String stream, PrintStream ps) {
		ConsoleMultiplexorStream cms = ensureStreamMux(engine, wc, stream);
		((WrapperPrintStream)cms.outs[0]).setPrintStream(ps);
	}

	private void setEchoPrefix(ViEngine engine, WritableSpiConfig wc, String stream, String prefix) {
		String pref = ViEngine.Core.transform(prefix, engine.getConfig().getNodeName());
		ConsoleMultiplexorStream cms = ensureStreamMux(engine, wc, stream);
		((WrapperPrintStream)cms.outs[0]).prefix = pref;
	}

	private ConsoleMultiplexorStream ensureStreamMux(ViEngine engine, WritableSpiConfig wc, String stream) {
		String mk = MUX_STREAM + stream;
		ConsoleMultiplexorStream cms = engine.getConfig().get(mk); 
		if (cms == null) {
			setSilentShutdown(engine, wc, getSilentShutdown(engine));
			cms = new ConsoleMultiplexorStream(new OutputStream[2]);
			ManagedProcess mp = engine.getConfig().getManagedProcess();
			@SuppressWarnings("resource")
            WrapperPrintStream ws = new WrapperPrintStream(getEchoPrefix(engine), "out".equals(stream) ? System.out : System.err, true);
			ws.silence = !getEchoEnabled(engine, stream);
			cms.outs[0] = ws;
			check(mp);
			if ("out".equals(stream)) {
				mp.bindStdOut(cms);
			}
			else {
				mp.bindStdErr(cms);
			}
			wc.setProp(mk, cms);
			wc.setProp(ViConf.ACTIVATED_FINALIZER_HOOK + mk, CloudContext.Helper.closeableFinalizer(cms));
		}
		return cms;
	}
	
	private String getEchoPrefix(ViEngine engine) {
		String p = engine.getConfig().get(ViConf.CONSOLE_ECHO_PREFIX);
		p = ViEngine.Core.transform(p, engine.getConfig().getNodeName());
		return p;
	}

	private boolean getSilentShutdown(ViEngine engine) {
		String key = ViConf.CONSOLE_SILENT_SHUTDOWN;
		return toBoolean(engine.getConfig().get(key));
	}

	private boolean getEchoEnabled(ViEngine engine, String stream) {
		String key = "out".equals(stream) ? ViConf.CONSOLE_STD_OUT_ECHO : ViConf.CONSOLE_STD_ERR_ECHO;
		return toBoolean(engine.getConfig().get(key));
	}

	private void check(ManagedProcess mp) {
		if (mp == null) {
			throw new IllegalArgumentException("No managed process in context");
		}
	}

	private boolean toBoolean(Object value) {
		if (value == null) {
			return false;
		}
		else {
			return Boolean.valueOf((String)value);
		}
	}
	
	private static void flushConsole(ViSpiConfig config) {
		ManagedProcess mp = config.getManagedProcess();
		if (mp != null) {
			try {
				mp.getExecutionService().submit(new Runnable() {
					@Override
					public void run() {
						System.out.flush();
						System.err.flush();
					}
				}).get();
				mp.consoleFlush();
			} catch (InterruptedException e) {
				// ignore
			} catch (ExecutionException e) {
				// ignore
			} catch (RejectedExecutionException e) {
				// ignore
			}
		}		
	}

	private static void silence(String stream, ViSpiConfig config) {
		String mk = MUX_STREAM + stream;
		ConsoleMultiplexorStream cms = config.get(mk);
		if (cms != null) {
			cms.silence();
		}		
	}
	
	static class Silencer implements Interceptor {

		@Override
		public void process(String name, Phase phase, QuorumGame game) {
			if (phase == Phase.PRE_KILL || phase == Phase.PRE_SHUTDOWN) {
				flushConsole(game);
				silence("out", game);
				silence("err", game);
			}			
		}

		@Override
		public void processAddHoc(String name, ViExecutor node) {
			// ok
		}
	}

	static class FinalFlush implements Interceptor {
		
		@Override
		public void process(String name, Phase phase, final QuorumGame game) {
			if (phase == Phase.PRE_KILL || phase == Phase.PRE_SHUTDOWN) {
				game.addUniqueProp(ViConf.ACTIVATED_FINALIZER_HOOK + "console-flush", new Runnable() {
					@Override
					public void run() {
						flushConsole(game);
					}
				});
			}			
		}
		
		@Override
		public void processAddHoc(String name, ViExecutor node) {
			// ok
		}
	}

	private static class ConsoleMultiplexorStream extends OutputStream {

		boolean silence = false; 
		OutputStream[] outs;
		
		public ConsoleMultiplexorStream(OutputStream... outs) {
			this.outs = outs;
		}

		public synchronized void silence() {
			try {
				flush();
				close();
			}
			catch(IOException e) {
				// ignore
			}
		}
		
		@Override
		public synchronized void write(int b) throws IOException {
			if (!silence) {
				for(OutputStream a: outs) {
					if (a != null) {
						a.write(b);
					}
				}
			}
		}

		@Override
		public synchronized void write(byte[] b) throws IOException {
			if (!silence) {
				for(OutputStream a: outs) {
					if (a != null) {
						a.write(b);
					}
				}
			}
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (!silence) {
				for(OutputStream a: outs) {
					if (a != null) {
						a.write(b, off, len);
					}
				}
			}
		}

		@Override
		public synchronized void flush() throws IOException {
			if (!silence) {
				for(OutputStream a: outs) {
					if (a != null) {
						a.flush();
					}
				}
			}
		}

		@Override
		public synchronized void close() throws IOException {
			if (!silence) {
				silence = true;
				for(OutputStream a: outs) {
					if (a != null) {
						a.close();
					}
				}
			}		
		}
	}
	
	// TODO make wrapper print stream shared utility class
	private static class WrapperPrintStream extends FilterOutputStream {

		String prefix;
		PrintStream printStream;
		ByteArrayOutputStream buffer;
		boolean ignoreClose;
		boolean silence;
		
		public WrapperPrintStream(String prefix, PrintStream printStream, boolean ignoreClose) {
			super(printStream);
			this.prefix = prefix;
			this.printStream = printStream;
			this.buffer = new ByteArrayOutputStream();
			this.ignoreClose = ignoreClose;
		}
		
		public void setPrintStream(PrintStream ps) {
			this.printStream = ps;
		}
		
		private void dumpBuffer() throws IOException {
			if (!silence) {
				String p = prefix;
				if (p != null) {
					printStream.append(prefix);
				}
				printStream.write(buffer.toByteArray());
				printStream.flush();
			}
			buffer.reset();
		}
		
		@Override
		public synchronized void write(int c) throws IOException {
			synchronized(printStream) {
				buffer.write(c);
				if (c == '\n') {
					dumpBuffer();
				}
			}
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			synchronized(printStream) {
				for (int i = 0; i != len; ++i) {
					if (b[off + i] == '\n') {
						writeByChars(b, off, len);
						return;
					}
				}
				buffer.write(b, off, len);
			}
		}

		private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
			for (int i = 0; i != len; ++i) {
				write(cbuf[off + i]);
			}
		}

		@Override
		public void close() throws IOException {
			super.flush();
			if (buffer.size() > 0) {
				dumpBuffer();
			}
			if (!ignoreClose) {
				printStream.close();
			}
		}
	}	
}
