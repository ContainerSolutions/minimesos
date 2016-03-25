package com.containersol.minimesos.main;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.*;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    @Parameter(names = "--marathonImageTag", description = "The tag of the Marathon Docker image.")
    private String marathonImageTag = null;

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images.")
    private String mesosImageTag = null;

    @Parameter(names = "--zooKeeperImageTag", description = "The tag of the ZooKeeper Docker images.")
    private String zooKeeperImageTag = null;

    @Parameter(names = "--timeout", description = "Time to wait for a container to get responsive, in seconds.")
    private Integer timeout = null;

    @Parameter(names = "--debug", description = "Enable debug logging.")
    private Boolean debug = null;

    /**
     * As number of agents can be determined either in config file or command line parameters, it defaults to invalid value.
     * Logic to select the actual number of agent is in the field getter
     */
    @Parameter(names = "--num-agents", description = "Number of agents to start")
    private int numAgents = -1;

    @Parameter(names = "--clusterConfig", description = "Path to file with cluster configuration. Defaults to minimesosFile")
    private String clusterConfigPath = ClusterConfig.DEFAULT_CONFIG_FILE;

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

    public String getMarathonImageTag() {
        return marathonImageTag;
    }

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public String getZooKeeperImageTag() {
        return zooKeeperImageTag;
    }

    public Boolean isExposedHostPorts() {
        return exposedHostPorts;
    }

    public void setExposedHostPorts(Boolean exposedHostPorts) {
        this.exposedHostPorts = exposedHostPorts;
    }

    public Integer getTimeout() {
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

    public String getClusterConfigPath() {
        return clusterConfigPath;
    }

    public void setClusterConfigPath(String clusterConfigPath) {
        this.clusterConfigPath = clusterConfigPath;
    }

    @Override
    public void execute() {
        if (debug != null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(Level.DEBUG);
            LOGGER.debug("Initialized debug logging");
        }

        LOGGER.debug("Executing up command");

        MesosCluster cluster = getCluster();
        if (cluster != null) {
            output.println("Cluster " + cluster.getClusterId() + " is already running");
            return;
        }

        ClusterArchitecture clusterArchitecture = getClusterArchitecture();

        startedCluster = new MesosCluster(clusterArchitecture);
        startedCluster.start();
        startedCluster.waitForState(state -> state != null);

        MarathonConfig marathonConfig = clusterArchitecture.getClusterConfig().getMarathon();
        if (marathonConfig != null) {
            LOGGER.debug("");
            startedCluster.getMarathonContainer().waitFor();
            List<AppConfig> apps = marathonConfig.getApps();
            for (AppConfig app : apps) {
                URL url = toUrl(app.getMarathonFile());
                startedCluster.getMarathonContainer().deployApp(url);
            }
        }

        startedCluster.printServiceUrls(output);

        ClusterRepository.saveClusterFile(startedCluster);
    }

    private URL toUrl(String marathonJsonPath) {
        URL url;
        try {
            LOGGER.debug("Converting '" + marathonJsonPath + "' to http(s) URL");
            url = new URL(marathonJsonPath);
        } catch (MalformedURLException e) {
            LOGGER.debug("Converting '" + marathonJsonPath + "' to file URL");
            url = toFileUrl(marathonJsonPath);
        }
        return url;
    }

    private URL toFileUrl(String marathonJsonPath) {
        File file = new File(MesosCluster.getHostDir(), marathonJsonPath);
        if (!file.exists()) {
            throw new MinimesosException("Invalid file path or URI for Marathon app: " + marathonJsonPath);
        } else {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new MinimesosException("Invalid file path or URI for Marathon app: " + marathonJsonPath);
            }
        }
    }

    /**
     * Getter for Cluster Config with caching logic.
     * This implementation cannot be used in multi-threaded mode
     *
     * @return configuration of the cluster from the file
     */
    public ClusterConfig getClusterConfig() {
        if (configFileFound != null) {
            return clusterConfig;
        }

        File clusterConfigFile = MesosCluster.getHostFile(getClusterConfigPath());
        if (clusterConfigFile.exists()) {
            configFileFound = true;
            ConfigParser configParser = new ConfigParser();
            try {
                clusterConfig = configParser.parse(FileUtils.readFileToString(clusterConfigFile));
            } catch (Exception e) {
                String msg = String.format("Failed to load cluster configuration from %s: %s", getClusterConfigPath(), e.getMessage());
                throw new MinimesosException(msg, e);
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
        ClusterConfig clusterConfig = getClusterConfig();
        if (clusterConfig == null) {
            // default cluster configuration is created
            clusterConfig = new ClusterConfig();
        }
        updateWithParameters(clusterConfig);


        ClusterArchitecture.Builder configBuilder = ClusterArchitecture.Builder.createCluster(clusterConfig);

        return configBuilder.build();
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
        if (getTimeout() != null) {
            clusterConfig.setTimeout(getTimeout());
        }

        boolean defaultMesosTags = (getMesosImageTag() == null);

        // ZooKeeper
        ZooKeeperConfig zooKeeperConfig = (clusterConfig.getZookeeper() != null) ? clusterConfig.getZookeeper() : new ZooKeeperConfig();
        if (getZooKeeperImageTag() != null) {
            zooKeeperConfig.setImageTag(getZooKeeperImageTag());
        }
        clusterConfig.setZookeeper(zooKeeperConfig);

        // Mesos Master
        MesosMasterConfig masterConfig = (clusterConfig.getMaster() != null) ? clusterConfig.getMaster() : new MesosMasterConfig();
        if (!defaultMesosTags) {
            masterConfig.setImageTag(getMesosImageTag());
        }
        clusterConfig.setMaster(masterConfig);

        // Marathon
        MarathonConfig marathonConfig = (clusterConfig.getMarathon() != null) ? clusterConfig.getMarathon() : new MarathonConfig();
        if (getMarathonImageTag() != null) {
            marathonConfig.setImageTag(getMarathonImageTag());
        }
        clusterConfig.setMarathon(marathonConfig);

        // creation of agents
        List<MesosAgentConfig> agentConfigs = clusterConfig.getAgents();
        List<MesosAgentConfig> updatedConfigs = new ArrayList<>();
        for (int i = 0; i < getNumAgents(); i++) {
            MesosAgentConfig agentConfig = (agentConfigs.size() > i) ? agentConfigs.get(i) : new MesosAgentConfig();
            if (!defaultMesosTags) {
                agentConfig.setImageTag(getMesosImageTag());
            }
            updatedConfigs.add(agentConfig);
        }
        clusterConfig.setAgents(updatedConfigs);

        // Consul
        ConsulConfig consulConfig = clusterConfig.getConsul();
        if (consulConfig == null) {
            consulConfig = new ConsulConfig();
        }
        clusterConfig.setConsul(consulConfig);

        //Registrator
        RegistratorConfig registratorConfig = clusterConfig.getRegistrator();
        if(registratorConfig ==null){
            registratorConfig = new RegistratorConfig();
        }
        clusterConfig.setRegistrator(registratorConfig);
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
