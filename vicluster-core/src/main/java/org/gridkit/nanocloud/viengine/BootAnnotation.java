package org.gridkit.nanocloud.viengine;


public class BootAnnotation {

    public enum Severity {
        WARNING,
        FATAL
    }

    public static String ERROR_PATTERN = Pragma.BOOT_ANNOTATION + "**.F*";
    public static String ANNOTATION_PATTERN = Pragma.BOOT_ANNOTATION + "**";

    private String bootPhase;
    private Severity severity;
    private String message;
    private Object[] arguments;
    private Throwable exception;

    public static BootAnnotation warning(String phase, String message, Object... arguments) {
        return new BootAnnotation(phase, Severity.WARNING, message, arguments);
    }

    public static void warning(PragmaWriter context, String message, Object... arguments) {
        new BootAnnotation((String)context.get(Pragma.BOOT_PHASE), Severity.WARNING, message, arguments).append(context);
    }

    public static BootAnnotation fatal(String phase, String message, Object... arguments) {
        return new BootAnnotation(phase, Severity.FATAL, message, arguments);
    }

    public static BootAnnotation fatal(String phase, Throwable e, String message, Object... arguments) {
        BootAnnotation mark = new BootAnnotation(phase, Severity.FATAL, message, arguments);
        mark.exception = e;
        return mark;
    }

    public static void fatal(PragmaWriter context, String message, Object... arguments) {
        new BootAnnotation((String)context.get(Pragma.BOOT_PHASE), Severity.FATAL, message, arguments).append(context);
    }

    public static void fatal(PragmaWriter context, Throwable e, String message, Object... arguments) {
        BootAnnotation mark = new BootAnnotation((String)context.get(Pragma.BOOT_PHASE), Severity.FATAL, message, arguments);
        mark.exception = e;
        mark.append(context);
    }

    protected BootAnnotation(String bootPhase, Severity severity, String message, Object[] arguments) {
        if (bootPhase == null) {
            throw new NullPointerException("bootPhase is null");
        }
        this.bootPhase = bootPhase;
        this.severity = severity;
        this.message = message;
        this.arguments = arguments;
        // validate string format
        String.format(message, arguments);
    }

    public void append(PragmaWriter writer) {
        int n = 0;
        while(true) {
            String key = Pragma.BOOT_ANNOTATION + bootPhase + "." + (severity == Severity.WARNING ? "W" : "F") + n;
            if (writer.get(key) == null) {
                writer.set(key, this);
                return;
            }
            ++n;
        }
    }

    public String getBootPhase() {
        return bootPhase;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getFormatedMessage() {
        return String.format(message, arguments);
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Throwable getExecption() {
        return exception;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity);
        sb.append(" [").append(bootPhase).append("] ");
        sb.append(String.format(message, arguments));
        return sb.toString();
    }
}
