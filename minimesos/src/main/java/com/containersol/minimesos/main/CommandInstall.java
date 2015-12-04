package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 * Installs a framework with Marathon
 */
@Parameters(commandDescription = "Install a framework with Marathon")
public class CommandInstall {

    @Parameter
    List<String> marathonFiles;

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    public String getMarathonFile() {
        String marathonFile = marathonFiles.get(0);
        if (marathonFile != null) {
            return marathonFile;
        } else {
            return null;
        }
    }

    public boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

}
