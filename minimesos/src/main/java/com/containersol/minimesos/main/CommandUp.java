package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.mesos.MesosContainer;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp {

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images (default=" + MesosContainer.MESOS_IMAGE_TAG + ").")
    private String mesosImageTag = MesosContainer.MESOS_IMAGE_TAG;

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--timeout", description = "Time within which a container should become responsive (default: " + MesosContainer.DEFAULT_TIMEOUT_SEC + ")")
    private int timeout = MesosContainer.DEFAULT_TIMEOUT_SEC;

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

    public int getTimeout() {
        return timeout;
    }
}
