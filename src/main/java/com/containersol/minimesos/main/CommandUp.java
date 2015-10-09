package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.mesos.MesosClusterConfig;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a mini mesos cluster")
public class CommandUp {

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images.")
    String mesosImageTag = MesosClusterConfig.MESOS_IMAGE_TAG;

    public String getMesosImageTag() {
        return mesosImageTag;
    }
}
