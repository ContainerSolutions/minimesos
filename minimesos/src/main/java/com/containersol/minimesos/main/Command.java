package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;

public interface Command {

    // TODO: this does not belong here
    boolean isExposedHostPorts();
    // TODO: this does not belong here
    boolean getStartConsul();

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