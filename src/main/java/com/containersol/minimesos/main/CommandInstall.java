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

    public String getMarathonFile() {
        String marathonFile = marathonFiles.get(0);
        if (marathonFile != null) {
            return marathonFile;
        } else {
            return null;
        }
    }

}
