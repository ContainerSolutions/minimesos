package com.containersol.minimesos.main;

public interface MinimesosCliCommand {

    /**
     * @deprecated having this method in the general command interface is wrong
     */
    boolean isExposedHostPorts();

    /**
     * @deprecated having this method in the general command interface is wrong
     */
    boolean getStartConsul();

}