package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.ClusterContainers;
import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersol.minimesos.mesos.MesosContainer;
import com.containersol.minimesos.mesos.MesosMaster;
import com.containersol.minimesos.mesos.MesosMasterExtended;
import com.containersol.minimesos.mesos.MesosSlave;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.github.dockerjava.api.DockerClient;
import org.apache.log4j.Logger;

import java.util.TreeMap;

/**
 * Parameters for the 'up' command
 */
@Parameters(separators = "=", commandDescription = "Create a minimesos cluster")
public class CommandUp implements Command {

    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    public static final String CLINAME = "up";

    @Parameter(names = "--exposedHostPorts", description = "Expose the Mesos and Marathon UI ports on the host level (we recommend to enable this on Mac (e.g. when using docker-machine) and disable on Linux).")
    private boolean exposedHostPorts = false;

    @Parameter(names = "--marathonImageTag", description = "The tag of the Marathon Docker image.")
    private String marathonImageTag = Marathon.MARATHON_IMAGE_TAG;

    @Parameter(names = "--mesosImageTag", description = "The tag of the Mesos master and agent Docker images.")
    private String mesosImageTag = MesosContainer.MESOS_IMAGE_TAG;

    @Parameter(names = "--zooKeeperImageTag", description = "The tag of the ZooKeeper Docker images.")
    private String zooKeeperImageTag = ZooKeeper.ZOOKEEPER_IMAGE_TAG;

    public String getMarathonImageTag() {
        return marathonImageTag;
    }

    @Parameter(names = "--timeout", description = "Time to wait for a container to get responsive, in seconds.")
    private int timeout = MesosContainer.DEFAULT_TIMEOUT_SEC;

    @Parameter(names = "--num-agents", description = "Number of agents to start")
    private int numAgents = 1;

    @Parameter(names = "--consul", description = "Start consul container")
    private boolean startConsul = false;

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

    public int getNumAgents() {
        return numAgents;
    }

    public boolean getStartConsul() {
        return startConsul;
    }

//            Main.clusterId = MesosCluster.readClusterId();
//            CliCommandHookExecutor.fireCallbacks("up", Main.clusterId, commandUp);
//            return;

    public MesosCluster execute() {
        MesosCluster cluster = ClusterRepository.loadCluster();
        if (cluster != null) {
            return cluster;
        }

        DockerClient dockerClient = DockerClientFactory.build();

        ClusterArchitecture.Builder configBuilder = new ClusterArchitecture.Builder(dockerClient)
                .withZooKeeper(getZooKeeperImageTag())
                .withMaster(zooKeeper -> new MesosMasterExtended(dockerClient, zooKeeper, MesosMaster.MESOS_MASTER_IMAGE, getMesosImageTag(), new TreeMap<>(), isExposedHostPorts()))
                .withContainer(zooKeeper -> new Marathon(dockerClient, zooKeeper, getMarathonImageTag(), isExposedHostPorts()), ClusterContainers.Filter.zooKeeper());

        for (int i = 0; i < getNumAgents(); i++) {
            configBuilder.withSlave(zooKeeper -> new MesosSlave(dockerClient, "ports(*):[33000-34000]", 5051, zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, getMesosImageTag()));
        }

        if (getStartConsul()) {
            configBuilder.withConsul();
        }

        cluster = new MesosCluster(configBuilder.build());
        cluster.start(getTimeout());
        cluster.waitForState(state -> state != null, 60);

        ClusterRepository.saveCluster(cluster);

        return cluster;
    }

}


