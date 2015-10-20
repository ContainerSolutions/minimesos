package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Installs a framework with Marathon
 */
@Parameters(separators = "=", commandDescription = "Install a framework with Marathon")
public class CommandInstall {

    @Parameter(description = "The Marathon JSON file.")
    String marathonFile;

    public String getMarathonFile() {
        return marathonFile;
    }

}
