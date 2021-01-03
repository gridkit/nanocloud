package org.gridkit.nanocloud.testutil;

import org.junit.Test;

public class JvmVersionCheck {

    public static boolean isJava8orBelow() {
        String version = System.getProperty("java.runtime.version");
        return version.startsWith("1.") ||
                version.startsWith("1.") ||
                version.startsWith("1.");

    }

    @Test
    public void testVersion() {
        System.out.println(isJava8orBelow());
    }

}
