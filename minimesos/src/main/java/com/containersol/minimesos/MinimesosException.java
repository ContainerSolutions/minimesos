package com.containersol.minimesos;

/**
 * Thrown when a minimesos command fails.
 */
public class MinimesosException extends RuntimeException {

    public MinimesosException(String message) {
        super(message);
    }

    public MinimesosException(String message, Throwable cause) {
        super(message, cause);
    }

}
