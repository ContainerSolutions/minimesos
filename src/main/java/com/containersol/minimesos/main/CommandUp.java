package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.mesos.MesosClusterConfig;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp {

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images.")
    private String mesosImageTag = MesosClusterConfig.MESOS_IMAGE_TAG;

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon ui ports on the host level.")
    private boolean exposedHostPorts = false;

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public boolean isExposedHostPorts() {
        return exposedHostPorts;
    }
}
