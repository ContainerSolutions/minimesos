package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.MesosContainer;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp implements MinimesosCliCommand {

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--marathonImageTag", description = "The tag of the Marathon Docker image.")
    private String marathonImageTag = Marathon.MARATHON_IMAGE_TAG;

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images.")
    private String mesosImageTag = MesosContainer.MESOS_IMAGE_TAG;

    @Parameter(names = "--zooKeeperImageTag", description = "The tag of the ZooKeeper Docker images.")
    private String zooKeeperImageTag = ZooKeeper.ZOOKEEPER_IMAGE_TAG;

    @Parameter(names = "--timeout", description = "Time to wait for a container to get responsive, in seconds.")
    private int timeout = MesosContainer.DEFAULT_TIMEOUT_SEC;

    @Parameter(names = "--num-agents", description = "Number of agents to start. Defaults to 1, unless configuration file is present")
    private int numAgents = -1;

    @Parameter(names = "--consul", description = "Start consul container")
    private boolean startConsul = false;

    @Parameter(names = "--config-file", description = "Path to configuration file")
    private String configFile = "minimesos.cfg";

    // holds loaded agents' configuration
    private List<String> agentResources = null;

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public String getZooKeeperImageTag() {
        return zooKeeperImageTag;
    }

    public boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

    public String getMarathonImageTag() {
        return marathonImageTag;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getNumAgents() {
        if (numAgents > 0) {
            return numAgents;
        } else if (agentResources != null) {
            return agentResources.size();
        } else {
            return 1;
        }
    }

    public boolean getStartConsul() {
        return startConsul;
    }

    /**
     * Reads definition of resources from configuration file
     *
     * @param minimesosHostDir current directory on the host, which is mapped to the same directory in minimesos container
     * @return strings with definition of resources
     */
    public List<String> loadAgentResources(File minimesosHostDir) {

        File file = new File(configFile);
        if (!file.exists()) {
            file = new File(minimesosHostDir, configFile);
        }

        if (file.exists()) {
            try {
                agentResources = Files.readLines(file, Charset.defaultCharset());
            } catch (IOException e) {
                throw new MinimesosException( "Failed to read content of " + file.getAbsolutePath(), e );
            }
        }

        return agentResources;

    }

}
