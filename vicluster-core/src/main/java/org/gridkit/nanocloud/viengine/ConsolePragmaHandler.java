package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.viengine.NodeConfigHelper.nodeActionFrom;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;

class ConsolePragmaHandler implements PragmaHandler {

    private static String MUX_STREAM = "#spi-console:mux-stream.";
    private static String SILENCER_HOOK = "console-silencer";

    @Override
    public void configure(PragmaWriter conext) {
        // do nothing
    }

    @Override
    public void init(PragmaWriter conext) {
        // do nothing

    }

    @Override
    public Object query(PragmaWriter context, String key) {
        if (ViConf.CONSOLE_STD_IN.equals(key)) {
            return termOf(context).getStdIn();
        } else {
            return context.get(key);
        }
    }

    @Override
    public void setup(PragmaWriter context, Map<String, Object> config) {
        apply(context, config);
    }

    @Override
    public void apply(PragmaWriter context, Map<String, Object> values) {
        for (String key: values.keySet()) {
            set(key, values.get(key), context);
        }
    }

    public void set(String key, Object value, PragmaWriter context) {
        if (ViConf.CONSOLE_FLUSH.equals(key)) {
            flushConsole(context);
        }
        else if (ViConf.CONSOLE_SILENT_SHUTDOWN.equals(key)) {
            setSilentShutdown(context, toBoolean(value));
        }
        else if (ViConf.CONSOLE_STD_IN.equals(key)) {
            // TODO
            //setInputPipe(context, (InputStream) value);
        }
        else if (ViConf.CONSOLE_STD_OUT.equals(key)) {
            setOutputPipe(context, "out", (OutputStream) value);
        }
        else if (ViConf.CONSOLE_STD_ERR.equals(key)) {
            setOutputPipe(context, "err", (OutputStream) value);
        }
        else if (ViConf.CONSOLE_STD_OUT_ECHO.equals(key)) {
            setOutputPipeEcho(context, "out", toBoolean(value));
        }
        else if (ViConf.CONSOLE_STD_ERR_ECHO.equals(key)) {
            setOutputPipeEcho(context, "err", toBoolean(value));
        }
        else if (ViConf.CONSOLE_STD_OUT_ECHO_STREAM.equals(key)) {
            setOutputPipeEchoStream(context, "out", new PrintStream((OutputStream) value));
        }
        else if (ViConf.CONSOLE_STD_ERR_ECHO_STREAM.equals(key)) {
            setOutputPipeEchoStream(context, "err", new PrintStream((OutputStream) value));
        }
        else if (ViConf.CONSOLE_ECHO_PREFIX.equals(key)) {
            setEchoPrefix(context, "out", (String)value);
            setEchoPrefix(context, "err", (String)value);
        }
        else {
            throw new IllegalArgumentException("Unknown pragma: " + key);
        }
    }

    private void setSilentShutdown(PragmaWriter context, boolean enabled) {
        context.set(Pragma.NODE_PRE_SHUTDOWN_HOOK + SILENCER_HOOK, enabled ? new Silencer() : null);
    }

    private static TextTerminalControl termOf(PragmaWriter context) {
        TextTerminalControl term = context.get(Pragma.RUNTIME_TEXT_TERMINAL);
        if (term == null) {
            throw new IllegalStateException("Console is not supported");
        }
        return term;
    }

    private void setOutputPipe(PragmaWriter context, String stream, OutputStream out) {
        ConsoleMultiplexorStream cms = ensureStreamMux(context, stream);
        cms.outs[1] = out;
    }

    private void setOutputPipeEcho(PragmaWriter context, String stream, boolean enabled) {
        ConsoleMultiplexorStream cms = ensureStreamMux(context, stream);
        ((WrapperPrintStream)cms.outs[0]).silence = !enabled;
    }

    private void setOutputPipeEchoStream(PragmaWriter context, String stream, PrintStream ps) {
        ConsoleMultiplexorStream cms = ensureStreamMux(context, stream);
        ((WrapperPrintStream)cms.outs[0]).setPrintStream(ps);
    }

    private void setEchoPrefix(PragmaWriter context, String stream, String prefix) {
        String pref = ViEngine.Core.transform(prefix, context.get(ViConf.NODE_NAME));
        ConsoleMultiplexorStream cms = ensureStreamMux(context, stream);
        ((WrapperPrintStream)cms.outs[0]).prefix = pref;
    }

    private ConsoleMultiplexorStream ensureStreamMux(PragmaWriter context, String stream) {
        String mk = MUX_STREAM + stream;
        ConsoleMultiplexorStream cms = context.get(mk);
        if (cms == null) {
            setSilentShutdown(context, getSilentShutdown(context));
            cms = new ConsoleMultiplexorStream(new OutputStream[2]);
            TextTerminalControl term = termOf(context);
            @SuppressWarnings("resource")
            WrapperPrintStream ws = new WrapperPrintStream(getEchoPrefix(context), "out".equals(stream) ? System.out : System.err, true);
            ws.silence = !getEchoEnabled(context, stream);
            cms.outs[0] = ws;
            if ("out".equals(stream)) {
                term.bindStdOut(cms);
            }
            else {
                term.bindStdErr(cms);
            }
            context.set(mk, cms);
            NodeConfigHelper.addPostShutdownHook(context, "ConsoleMuxFinalizer" + mk, nodeActionFrom(CloudContext.Helper.closeableFinalizer(cms)));
        }
        return cms;
    }

    private String getEchoPrefix(PragmaWriter context) {
        String p = NodeConfigHelper.transform((String)context.get(ViConf.CONSOLE_ECHO_PREFIX), (String)context.get(ViConf.NODE_NAME));
        return p;
    }

    private boolean getSilentShutdown(PragmaWriter context) {
        String key = ViConf.CONSOLE_SILENT_SHUTDOWN;
        return toBoolean(context.get(key));
    }

    private boolean getEchoEnabled(PragmaWriter context, String stream) {
        String key = "out".equals(stream) ? ViConf.CONSOLE_STD_OUT_ECHO : ViConf.CONSOLE_STD_ERR_ECHO;
        return toBoolean(context.get(key));
    }

    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        else {
            return Boolean.valueOf((String)value);
        }
    }

    private static void flushConsole(PragmaWriter context) {

        TextTerminalControl term = termOf(context);

        term.consoleFlush();

        // flush dericted streams if any
        flushMux(context.<ConsoleMultiplexorStream>get(MUX_STREAM + "out"));
        flushMux(context.<ConsoleMultiplexorStream>get(MUX_STREAM + "err"));
    }

    private static void flushMux(ConsoleMultiplexorStream cms) {
        if (cms != null && cms.outs != null) {
            for(OutputStream out: cms.outs) {
                if (out != null) {
                    try {
                        out.flush();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    private static void silence(String stream, PragmaWriter context) {
        String mk = MUX_STREAM + stream;
        ConsoleMultiplexorStream cms = context.get(mk);
        if (cms != null) {
            cms.silence();
        }
    }

    static class Silencer implements  NodeAction {

        @Override
        public void run(PragmaWriter context) throws ExecutionException {
            flushConsole(context);
            silence("out", context);
            silence("err", context);
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
