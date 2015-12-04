package com.containersol.minimesos.main;

import com.beust.jcommander.JCommander;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.marathon.MarathonClient;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;

/**
 * Main method for interacting with minimesos.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static CommandUp commandUp;

    public static void main(String[] args)  {
        JCommander jc = new JCommander();
        jc.setProgramName("minimesos");

        commandUp = new CommandUp();
        CommandDestroy commandDestroy = new CommandDestroy();
        CommandHelp commandHelp = new CommandHelp();
        CommandInfo commandInfo = new CommandInfo();
        CommandInstall commandInstall = new CommandInstall();

        jc.addCommand("up", commandUp);
        jc.addCommand("destroy", commandDestroy);
        jc.addCommand("help", commandHelp);
        jc.addCommand("info", commandInfo);
        jc.addCommand("install", commandInstall );
        jc.parseWithoutValidation(args);

        String clusterId = MesosCluster.readClusterId();
        MesosCluster.checkStateFile(clusterId);
        clusterId = MesosCluster.readClusterId();

        if (jc.getParsedCommand() == null) {
            if (clusterId != null) {
                MesosCluster.printServiceUrl(clusterId, "master", commandUp.isExposedHostPorts());
                MesosCluster.printServiceUrl(clusterId, "marathon", commandUp.isExposedHostPorts());
            } else {
                jc.usage();
            }
            return;
        }

        switch (jc.getParsedCommand()) {
            case "up":
                doUp();
                break;
            case "info":
                printInfo();
                break;
            case "destroy":
                MesosCluster.destroy();
                break;
            case "install":
                String marathonFile = commandInstall.getMarathonFile();
                String marathonIp = MesosCluster.getContainerIp(MesosCluster.readClusterId(), "marathon");
                System.out.println(String.format("Installing %s on maraphon %s", marathonFile, marathonIp));
                MarathonClient.deployFramework(marathonIp, marathonFile);
                break;
            case "help":
                jc.usage();
        }
    }

    private static void doUp() {

        String clusterId = MesosCluster.readClusterId();

        boolean exposedHostPorts = commandUp.isExposedHostPorts();
        String mesosImageTag = commandUp.getMesosImageTag();

        if (clusterId == null) {

            DockerClient dockerClient = DockerClientFactory.build();

            ClusterArchitecture config = new ClusterArchitecture.Builder(dockerClient)
                    .withZooKeeper()
                    .withMaster(zooKeeper -> new MesosMasterExtended( dockerClient, zooKeeper, MesosMaster.MESOS_MASTER_IMAGE, mesosImageTag, new TreeMap<>(), exposedHostPorts))
                    .withSlave(zooKeeper -> new MesosSlaveExtended( dockerClient, "ports(*):[33000-34000]", "5051", zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, mesosImageTag))
                    .withMarathon(zooKeeper -> new Marathon(dockerClient, zooKeeper, exposedHostPorts))
                    .build();

            MesosCluster cluster = new MesosCluster(config);

            cluster.start();
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

}
