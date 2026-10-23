package com.reduan.vibechess.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Sanity unit test verifying the pure Java :game-core module compiles and runs on Java 17.
 */
public class CoreModuleSanityTest {

    @Test
    public void javaModulePackageIsCorrect() {
        assertEquals("com.reduan.vibechess.core", CoreModuleSanityTest.class.getPackageName());
    }
}
