package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp implements Command {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommandUp.class);

    public static final String CLINAME = "up";

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private Boolean exposedHostPorts = null;

    /**
     * As number of agents can be determined either in config file or command line parameters, it defaults to invalid value.
     * Logic to select the actual number of agent is in the field getter
     */
    @Parameter(names = "--num-agents", description = "Number of agents to start")
    private int numAgents = -1;

    @Parameter(names = "--clusterConfig", description = "Path to file with cluster configuration. Defaults to minimesosFile")
    private String clusterConfigPath = ClusterConfig.DEFAULT_CONFIG_FILE;

    private MesosCluster startedCluster = null;

    private PrintStream output = System.out;

    private MesosClusterContainersFactory mesosClusterFactory;

    public CommandUp() {
        mesosClusterFactory = new MesosClusterContainersFactory();
    }

    public Boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

    public void setExposedHostPorts(Boolean exposedHostPorts) {
        this.exposedHostPorts = exposedHostPorts;
    }

    /**
     * Number of agents defined in configuration file, unless overwritten in the command line.
     * If neither is set, 1 is returned
     *
     * @return Number of agents to create
     */
    public int getNumAgents() {
        int numAgents;
        if (this.numAgents > 0) {
            numAgents = this.numAgents;
        } else {
            ClusterConfig clusterConfig = readClusterConfigFromMinimesosFile();
            if ((clusterConfig != null) && (clusterConfig.getAgents() != null) && (clusterConfig.getAgents().size() > 0)) {
                numAgents = clusterConfig.getAgents().size();
            } else {
                numAgents = 1;
            }
        }
        return numAgents;
    }

    public String getClusterConfigPath() {
        return clusterConfigPath;
    }

    public void setClusterConfigPath(String clusterConfigPath) {
        this.clusterConfigPath = clusterConfigPath;
    }

    @Override
    public void execute() {
        LOGGER.debug("Executing up command");

        MesosCluster cluster = getCluster();
        if (cluster != null) {
            output.println("Cluster " + cluster.getClusterId() + " is already running");
            return;
        }
        ClusterConfig clusterConfig = readClusterConfigFromMinimesosFile();
        updateWithParameters(clusterConfig);

        startedCluster = mesosClusterFactory.createMesosCluster(clusterConfig);
        // save cluster ID first, so it becomes available for 'destroy' even if its part failed to start
        ClusterRepository.saveClusterFile(startedCluster);

        startedCluster.start();
        startedCluster.waitForState(state -> state != null);

        new CommandInfo().execute();
    }

    /**
     * Reads ClusterConfig from minimesosFile.
     *
     * @return configuration of the cluster from the minimesosFile
     * @throws MinimesosException if minimesosFile is not found or malformed
     */
    public ClusterConfig readClusterConfigFromMinimesosFile() {
        InputStream clusterConfigFile = MesosCluster.getInputStream(getClusterConfigPath());
        if (clusterConfigFile != null) {
            ConfigParser configParser = new ConfigParser();
            try {
                return configParser.parse(IOUtils.toString(clusterConfigFile));
            } catch (Exception e) {
                String msg = String.format("Failed to load cluster configuration from %s: %s", getClusterConfigPath(), e.getMessage());
                throw new MinimesosException(msg, e);
            }
        }
        throw new MinimesosException("No minimesosFile found at '" + getClusterConfigPath() + "'. Please generate one with 'minimesos init'");
    }

    /**
     * Adjust cluster configuration according to CLI parameters
     *
     * @param clusterConfig cluster configuration to update
     */
    public void updateWithParameters(ClusterConfig clusterConfig) {

        if (isExposedHostPorts() != null) {
            clusterConfig.setExposePorts(isExposedHostPorts());
        }

        // ZooKeeper
        if (clusterConfig.getZookeeper() == null) {
            clusterConfig.setZookeeper(new ZooKeeperConfig());
        }

        // Mesos Master
        if (clusterConfig.getMaster() == null) {
            clusterConfig.setMaster(new MesosMasterConfig());
        }

        // creation of agents
        List<MesosAgentConfig> agentConfigs = clusterConfig.getAgents();
        List<MesosAgentConfig> updatedConfigs = new ArrayList<>();
        for (int i = 0; i < getNumAgents(); i++) {
            MesosAgentConfig agentConfig = (agentConfigs.size() > i) ? agentConfigs.get(i) : new MesosAgentConfig();
            updatedConfigs.add(agentConfig);
        }
        clusterConfig.setAgents(updatedConfigs);

    }

    public MesosCluster getCluster() {
        return (startedCluster != null) ? startedCluster : ClusterRepository.loadCluster(new MesosClusterContainersFactory());
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

    public void setMesosClusterFactory(MesosClusterContainersFactory mesosClusterFactory) {
        this.mesosClusterFactory = mesosClusterFactory;
    }

}
