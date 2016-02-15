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

    private static Logger LOGGER = Logger.getLogger(CommandInstall.class);

    public static final String CLINAME = "install";

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--marathonFile", description = "Marathon JSON app install file location")
    private String marathonFile = "";

    /**
     * Getting content of <code>marathonFile</code>, if provided, or standard input
     * @param minimesosHostDir current directory on the host, which is mapped to the same directory in minimesos container
     * @return content of the file or standard input
     */
    public String getMarathonJson( File minimesosHostDir ) {
        String fileContents = "";
        Scanner scanner;
        try {

            if (!marathonFile.isEmpty()) {

                File jsonFile = new File( marathonFile );
                if( !jsonFile.exists() ) {
                    jsonFile = new File( minimesosHostDir, marathonFile );
                    if( !jsonFile.exists() ) {
                        String msg = String.format("Neither %s nor %s exist", new File( marathonFile ).getAbsolutePath(), jsonFile.getAbsolutePath() );
                        throw new MinimesosException( msg );
                    }
                }

                scanner = new Scanner(new FileReader(jsonFile));
            } else {
                // TODO: this causes https://github.com/ContainerSolutions/minimesos/issues/224
                scanner = new Scanner(System.in);
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

    @Override
    public boolean getStartConsul() {
        return false;
    }

    public void execute() {

        String marathonJson = getMarathonJson(MesosCluster.getHostDir());

        MesosCluster cluster = ClusterRepository.loadCluster();
        if( cluster != null ) {
            cluster.deployMarathonApp( marathonJson );
        } else {
            throw new MinimesosException("Running cluster is not found");
        }

    }

}
