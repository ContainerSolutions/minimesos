package com.containersol.minimesos.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;

/**
 * Main method for interacting with minimesos.
 */
@Parameters(separators = "=", commandDescription = "Global options")
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static CommandUp commandUp;

    @Parameter(names = {"--help", "-help", "-?", "-h"}, help = true)
    private static boolean help = false;

    public static void main(String[] args)  {

        JCommander jc = new JCommander(new Main());
        jc.setProgramName("minimesos");

        commandUp = new CommandUp();
        CommandDestroy commandDestroy = new CommandDestroy();
        CommandHelp commandHelp = new CommandHelp();
        CommandInfo commandInfo = new CommandInfo();
        CommandInstall commandInstall = new CommandInstall();
        CommandState commandState = new CommandState();

        jc.addCommand("up", commandUp);
        jc.addCommand("destroy", commandDestroy);
        jc.addCommand("help", commandHelp);
        jc.addCommand("info", commandInfo);
        jc.addCommand("install", commandInstall );
        jc.addCommand("state", commandState);

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse parameters. " + e.getMessage() + "\n" );
            jc.usage();
            System.exit(1);
        }

        String clusterId = MesosCluster.readClusterId();
        MesosCluster.checkStateFile(clusterId);
        clusterId = MesosCluster.readClusterId();

        if(help) {
            jc.usage();
            return;
        }

        if (jc.getParsedCommand() == null) {
            if (clusterId != null) {
                MesosCluster.printServiceUrl(clusterId, "master", commandUp.isExposedHostPorts());
                MesosCluster.printServiceUrl(clusterId, "marathon", commandUp.isExposedHostPorts());
            } else {
                jc.usage();
            }
            return;
        }

        try {
            switch (jc.getParsedCommand()) {
                case "up":
                    doUp(commandUp.getTimeout());
                    break;
                case "info":
                    printInfo();
                    break;
                case "destroy":
                    MesosCluster.destroy();
                    break;
                case "install":
                    String marathonFilePath = commandInstall.getMarathonFile();
                    if(StringUtils.isBlank(marathonFilePath) ) {
                        jc.usage();
                    } else {
                        MesosCluster.executeMarathonTask( clusterId, marathonFilePath );
                    }
                    break;
                case "state":
                    printState(commandState.getAgent());
                    break;
                case "help":
                    jc.usage();
            }
        } catch (MinimesosException mme) {
            LOGGER.error("ERROR: " + mme.getMessage());
            System.exit(1);
        } catch (Exception e) {
            LOGGER.error("ERROR: " + e.toString() );
            System.exit(1);
        }

    }

    /**
     * @param timeout in seconds
     */
    private static void doUp(int timeout) {

        String clusterId = MesosCluster.readClusterId();

        boolean exposedHostPorts = commandUp.isExposedHostPorts();
        String marathonImageTag = commandUp.getMarathonImageTag();
        String mesosImageTag = commandUp.getMesosImageTag();
        String zooKeeperImageTag = commandUp.getZooKeeperImageTag();

        if (clusterId == null) {

            DockerClient dockerClient = DockerClientFactory.build();

            ClusterArchitecture.Builder configBuilder = new ClusterArchitecture.Builder(dockerClient)
                    .withZooKeeper(zooKeeperImageTag)
                    .withConsul()
                    .withMaster(zooKeeper -> new MesosMasterExtended( dockerClient, zooKeeper, MesosMaster.MESOS_MASTER_IMAGE, mesosImageTag, new TreeMap<>(), exposedHostPorts))
                    .withContainer( zooKeeper -> new Marathon(dockerClient, zooKeeper, marathonImageTag, exposedHostPorts), ClusterContainers.Filter.zooKeeper() );
            for (int i = 0; i < commandUp.getNumAgents(); i++) {
                configBuilder.withSlave(zooKeeper -> new MesosSlaveExtended( dockerClient, "ports(*):[33000-34000]", "5051", zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, mesosImageTag));
            }
            MesosCluster cluster = new MesosCluster(configBuilder.build());

            cluster.start(timeout);
            cluster.writeClusterId();

        }

        clusterId = MesosCluster.readClusterId();
        MesosCluster.printServiceUrl(clusterId, "master", exposedHostPorts);
        MesosCluster.printServiceUrl(clusterId, "marathon", exposedHostPorts);

    }

    private static void printInfo() {
        String clusterId = MesosCluster.readClusterId();
        if (clusterId != null) {
            LOGGER.info("Minimesos cluster is running");
            LOGGER.info("Mesos version: " + MesosContainer.MESOS_IMAGE_TAG.substring(0, MesosContainer.MESOS_IMAGE_TAG.indexOf("-")));
        } else {
            LOGGER.info("Minimesos cluster is not running");
        }
    }

    private static void printState(String agent) {
        String clusterId = MesosCluster.readClusterId();
        String stateInfo = (StringUtils.isEmpty(agent)) ? MesosCluster.getClusterStateInfo(clusterId) : MesosCluster.getContainerStateInfo(clusterId);
        if( stateInfo != null ) {
            LOGGER.info(stateInfo);
        } else {
            throw new MinimesosException("Did not find the cluster or requested container");
        }

    }

}
