package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
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

    @Override
    public boolean getStartConsul() {
        return false;
    }

    public void execute() {

//                    cluster = MesosCluster.loadCluster(clusterId);
//                    String marathonJson = commandInstall.getMarathonJson(MesosCluster.getHostDir());
//                    if (StringUtils.isBlank(marathonJson)) {
//                        jc.usage();
//                    } else {
//                        cluster.getMarathonContainer().deployApp(marathonJson);
//                    }
//                    break;


    }
}
