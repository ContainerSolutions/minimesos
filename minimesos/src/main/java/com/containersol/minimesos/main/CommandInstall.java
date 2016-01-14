package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

/**
 * Installs a framework with Marathon
 */
@Parameters(commandDescription = "Install a framework with Marathon")
public class CommandInstall {
    private static Logger LOGGER = Logger.getLogger(CommandInstall.class);

    @Parameter
    List<String> marathonJson;

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--marathonFile", description = "Marathon JSON app install file location")
    private String marathonFile = "";

    public String getMarathonJson() {
        String fileContents = "";
        Scanner scanner;
        try {
            if (!marathonFile.isEmpty()) {
                scanner = new Scanner(new FileReader(marathonFile));
            } else {
                scanner = new Scanner(new InputStreamReader(System.in));
            }
            while (scanner.hasNextLine()) {
                fileContents = fileContents.concat(scanner.nextLine());
            }
            return fileContents;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }

    public boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

}
