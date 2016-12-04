package com.containersol.minimesos.util;

/**
 * Utility for detecting the runtime environment.
 */
public class Environment {

    private Environment() {

    }

    /**
     * Checks if minimesos cli runs in JVM on Mac OS X.
     *
     * @return true if it runs on Mac OS X without Docker
     */
    public static boolean isRunningInJvmOnMacOsX() {
        return System.getProperty("os.name").contains("Mac OS X");
    }

    /**
     * Checks if minimesos cli runs in Docker on Mac
     *
     * @return true if MINIMESOS_OS env var is set by bin/minimesos
     */
    public static boolean isRunningInDockerOnMac() {
        return System.getenv("MINIMESOS_OS") != null && System.getenv("MINIMESOS_OS").contains("Mac OS X");
    }
}
