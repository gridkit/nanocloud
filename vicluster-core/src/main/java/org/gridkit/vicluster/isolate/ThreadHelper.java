package org.gridkit.vicluster.isolate;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

class ThreadHelper {

    public static boolean stopThread(String name, Thread thread) {
        return stopWithThreadDoom(name, thread) || stopWithThreadDeath(thread);
    }

    private static boolean stopWithThreadDoom(String name, Thread thread) {

        // use reflection because method was removed in Java 11
        try {
            Method m = thread.getClass().getMethod("stop", Exception.class);
            m.setAccessible(true);
            m.invoke(thread, new ThreadDoomError(name));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean stopWithThreadDeath(Thread thread) {
        try {
            thread.stop();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @SuppressWarnings("serial")
    private static class ThreadDoomError extends ThreadDeath {

        private final String name;

        public ThreadDoomError(String name) {
            this.name = name;
        }

        @Override
        public Throwable getCause() {
            return null;
        }

        @Override
        public String toString() {
            return "Isolate [" + name + "] has been terminated";
        }

        @Override
        public void printStackTrace() {
        }

        @Override
        public void printStackTrace(PrintStream s) {
        }

        @Override
        public void printStackTrace(PrintWriter s) {
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return null;
        }
    }
}
