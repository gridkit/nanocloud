package org.gridkit.zerormi;

import java.lang.reflect.Method;

public class JdkUtils {

    public static void addSuppressedIfSupported(Throwable t, Throwable suppressed){
        try {
            Method method = Throwable.class.getMethod("addSuppressed", Throwable.class);
            method.invoke(t, suppressed);
        } catch (Exception e) {
            // ignore any problems: for example NoSuchMethodException
        }
    }
}
