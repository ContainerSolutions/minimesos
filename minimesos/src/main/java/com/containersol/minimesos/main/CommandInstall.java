package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Scanner;

/**
 * Installs a framework with Marathon
 */
@Parameters(commandDescription = "Install a framework with Marathon")
public class CommandInstall implements Command {

    public static final String CLINAME = "install";

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--marathonFile", description = "Marathon JSON app install file location. Either this or --stdin parameter must be used")
    private String marathonFile = null;

    @Parameter(names = "--stdin", description = "Use JSON from standard import. Allow piping JSON from other processes. Either this or --marathonFile parameter must be used")
    private boolean stdin = false;

    /**
     * Getting content of <code>marathonFile</code>, if provided, or standard input
     *
     * @return content of the file or standard input
     */
    public String getMarathonJson() throws IOException {

        String fileContents = "";
        Scanner scanner;

        if (marathonFile != null && !marathonFile.isEmpty()) {

            File jsonFile = MesosCluster.getHostFile(marathonFile);
            if (!jsonFile.exists()) {
                String msg = String.format("Neither %s nor %s exist", new File(marathonFile).getAbsolutePath(), jsonFile.getAbsolutePath());
                throw new MinimesosException(msg);
            }

            scanner = new Scanner(new FileReader(jsonFile));

        } else if (stdin) {
            scanner = new Scanner(System.in);
        } else {
            throw new MinimesosException("Neither --marathonFile nor --stdin parameters are provided");
        }

        while (scanner.hasNextLine()) {
            fileContents = fileContents.concat(scanner.nextLine());
        }

        return fileContents;

    }

    @Override
    public void execute() throws MinimesosException {
        String marathonJson;
        try {
            marathonJson = getMarathonJson();
        } catch (IOException e) {
            throw new MinimesosException("Failed to read JSON", e);
        }

        MesosCluster cluster = ClusterRepository.loadCluster();
        if (cluster != null) {
            cluster.deployMarathonApp(marathonJson);
        } else {
            throw new MinimesosException("Running cluster is not found");
        }

    }

    @Override
    public boolean validateParameters() {
        return stdin || (marathonFile != null && !marathonFile.isEmpty());
    }

    @Override
    public String getName() {
        return CLINAME;
    }

    public void setMarathonFile(String marathonFile) {
        this.marathonFile = marathonFile;
    }
}
