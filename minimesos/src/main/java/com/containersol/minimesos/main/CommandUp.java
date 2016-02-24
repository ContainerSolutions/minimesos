package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.*;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp implements Command {

    public static final String CLINAME = "up";

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--marathonImageTag", description = "The tag of the Marathon Docker image.")
    private String marathonImageTag = Marathon.MARATHON_IMAGE_TAG;

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images.")
    private String mesosImageTag = MesosContainerConfig.MESOS_IMAGE_TAG;

    @Parameter(names = "--zooKeeperImageTag", description = "The tag of the ZooKeeper Docker images.")
    private String zooKeeperImageTag = ZooKeeperConfig.ZOOKEEPER_IMAGE_TAG;

    public String getMarathonImageTag() {
        return marathonImageTag;
    }

    @Parameter(names = "--timeout", description = "Time to wait for a container to get responsive, in seconds.")
    private int timeout = MesosCluster.DEFAULT_TIMEOUT_SECS;

    @Parameter(names = "--num-agents", description = "Number of agents to start")
    private int numAgents = -1;

    @Parameter(names = "--consul", description = "Start consul container")
    private boolean startConsul = false;

    @Parameter(names = "--clusterConfig", description = "Path to file with cluster configuration. Defaults to minimesosFile")
    private String clusterConfigPath = "minimesosFile";

    /**
     * Indicates is configurationFile was found
     */
    private Boolean configFileFound = null;
    private ClusterConfig clusterConfig = null;

    private MesosCluster startedCluster = null;
    private PrintStream output = System.out;


    public CommandUp() {
    }

    public CommandUp(PrintStream ps) {
        output = ps;
    }

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public String getZooKeeperImageTag() {
        return zooKeeperImageTag;
    }

    public boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * Number of agents defined in configuration file, unless overwritten in the command line.
     * If neither is set, 1 is returned
     *
     * @return Number of agents to create
     */
    public int getNumAgents() {
        if (numAgents > 0) {
            return numAgents;
        } else {
            ClusterConfig clusterConfig = getClusterConfig();
            if ((clusterConfig != null) && (clusterConfig.getAgents() != null) && (clusterConfig.getAgents().size() > 0)) {
                return clusterConfig.getAgents().size();
            } else {
                return 1;
            }
        }
    }

    public boolean getStartConsul() {
        return startConsul;
    }

    public String getClusterConfigPath() {
        return clusterConfigPath;
    }

    public void setClusterConfigPath(String clusterConfigPath) {
        this.clusterConfigPath = clusterConfigPath;
    }

    @Override
    public void execute() {

        MesosCluster cluster = getCluster();
        if (cluster != null) {
            output.println("Cluster " + cluster.getClusterId() + " is already running");
            return;
        }

        ClusterArchitecture clusterArchitecture = getClusterArchitecture();

        startedCluster = new MesosCluster(clusterArchitecture);
        startedCluster.start(getTimeout());
        startedCluster.waitForState(state -> state != null, 60);
        startedCluster.setExposedHostPorts( isExposedHostPorts() );

        startedCluster.printServiceUrls(output);

        ClusterRepository.saveClusterFile(startedCluster);

    }

    /**
     * Getter for Cluster Config with caching logic.
     * This implementation cannot be used in multi-threaded mode
     *
     * @return configuration of the cluster from the file
     */
    public ClusterConfig getClusterConfig() {

        if( configFileFound != null ) {
            return clusterConfig;
        }

        File clusterConfigFile = new File(getClusterConfigPath());
        if (clusterConfigFile.exists()) {
            configFileFound = true;
            ConfigParser configParser = new ConfigParser();
            try {
                clusterConfig = configParser.parse(FileUtils.readFileToString(clusterConfigFile));
            } catch (IOException e) {
                throw new MinimesosException("Failed to load cluster configuration from " + clusterConfigFile.getAbsolutePath(), e);
            }
        } else {
            configFileFound = false;
        }

        return clusterConfig;

    }

    /**
     * Creates cluster architecture based on the command parameters and configuration file
     *
     * @return cluster architecture
     */
    public ClusterArchitecture getClusterArchitecture() {

        DockerClient dockerClient = DockerClientFactory.build();
        ClusterArchitecture.Builder configBuilder = new ClusterArchitecture.Builder(dockerClient);

        ClusterConfig clusterConfig = getClusterConfig();
        if( clusterConfig == null ) {
            clusterConfig = new ClusterConfig();
        }
        configBuilder = createCluster(configBuilder, clusterConfig);

        return configBuilder.build();

    }

    /**
     * Creates architecture for default cluster configuration
     *
     * @param configBuilder builder to extend
     * @param clusterConfig loaded from file cluster configuration
     * @return reference to the given builder, so the method call can be chained
     */
    private ClusterArchitecture.Builder createCluster(ClusterArchitecture.Builder configBuilder, ClusterConfig clusterConfig) {

        DockerClient dockerClient = configBuilder.getDockerClient();

        // ZooKeeper configuration
        ZooKeeperConfig zooKeeperConfig = (clusterConfig.getZookeeper() != null) ? clusterConfig.getZookeeper() : new ZooKeeperConfig();
        zooKeeperConfig.setImageTag(getZooKeeperImageTag());
        configBuilder.withZooKeeper(zooKeeperConfig);

        // Mesos Master
        MesosMasterConfig masterConfig = (clusterConfig.getMaster() != null) ? clusterConfig.getMaster() : new MesosMasterConfig();
        masterConfig.setImageTag(getMesosImageTag());
        masterConfig.setExposedHostPort(isExposedHostPorts());
        configBuilder.withMaster(zooKeeper -> new MesosMaster(dockerClient, zooKeeper, masterConfig));

        // TODO: zooKeeper
        configBuilder.withContainer(zooKeeper -> new Marathon(dockerClient, zooKeeper, getMarathonImageTag(), isExposedHostPorts()), ClusterContainers.Filter.zooKeeper());

        // creation of agents
        List<MesosAgentConfig> agentConfigs = clusterConfig.getAgents();
        for (int i = 0; i < getNumAgents(); i++) {
            MesosAgentConfig agentConfig = (agentConfigs.size()>i) ? agentConfigs.get(i) : new MesosAgentConfig();
            agentConfig.setImageTag( getMesosImageTag() );
            configBuilder.withSlave(zooKeeper -> new MesosSlave(dockerClient, zooKeeper, agentConfig));
        }

        if (getStartConsul()) {
            configBuilder.withConsul();
        }

        return configBuilder;

    }

    public MesosCluster getCluster() {
        return (startedCluster != null) ? startedCluster : ClusterRepository.loadCluster();
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

}


