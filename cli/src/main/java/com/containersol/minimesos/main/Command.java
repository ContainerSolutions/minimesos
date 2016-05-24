package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;

public interface Command {

    /**
     * Validates combination of command parameters
     *
     * @return true if command parameters are valid
     */
    boolean validateParameters();

    /**
     * @return name of the command
     */
    String getName();

    /**
     * Executes the command
     */
    void execute() throws MinimesosException;

}