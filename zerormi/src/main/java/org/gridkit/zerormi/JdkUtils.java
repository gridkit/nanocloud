package org.gridkit.zerormi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class JdkUtils {

    private static Field throwableCauseField;

    public static boolean addSuppressedIfSupported(Throwable t, Throwable suppressed){
        try {
            Method method = Throwable.class.getMethod("addSuppressed", Throwable.class);
            method.invoke(t, suppressed);
            return true;
        } catch (Exception e) {
            // ignore any problems: for example NoSuchMethodException
            return false;
        }
    }

    public static void replaceCause(Throwable e, Throwable c) {
        synchronized (JdkUtils.class) {
            if (throwableCauseField == null) {
                try {
                    Field cause = Throwable.class.getDeclaredField("cause");
                    cause.setAccessible(true);
                    throwableCauseField = cause;
                } catch (Exception ee) {
                    // ignore
                }
            }
        }
        if (throwableCauseField != null) {
            try {
                throwableCauseField.set(e, c);
            } catch (Exception e1) {
                // ignore
            }
        }
    }

}
