package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class DynamicClusterTest {

    private static final boolean EXPOSED_PORTS = false;

    protected static final DockerClient dockerClient = DockerClientFactory.build();

    @Test
    public void noMarathonTest() {

        MesosMasterConfig masterConfig = new MesosMasterConfig();

        ClusterArchitecture config = new ClusterArchitecture.Builder(dockerClient)
                .withZooKeeper()
                .withMaster(zooKeeper -> new MesosMaster(dockerClient, zooKeeper, masterConfig ))
                .withAgent(zooKeeper -> new MesosAgent(dockerClient, zooKeeper ))
                .build();

        MesosCluster cluster = new MesosCluster(config);
        cluster.setExposedHostPorts(EXPOSED_PORTS);

        cluster.start();
        String clusterId = cluster.getClusterId();

        assertNotNull( "Cluster ID must be set", clusterId );

        // this should not throw any exceptions
        cluster.stop();

    }

    @Test
    public void stopWithNewContainerTest() {

        MesosMasterConfig masterConfig = new MesosMasterConfig();

        ClusterArchitecture config = new ClusterArchitecture.Builder(dockerClient)
                .withZooKeeper()
                .withMaster(zooKeeper -> new MesosMaster(dockerClient, zooKeeper, masterConfig ))
                .withAgent(zooKeeper -> new MesosAgent(dockerClient, zooKeeper))
                .build();

        MesosCluster cluster = new MesosCluster(config);
        cluster.setExposedHostPorts(EXPOSED_PORTS);
        cluster.start();

        ZooKeeper zooKeeper = cluster.getZkContainer();
        MesosAgent extraAgent = new MesosAgent(dockerClient, zooKeeper);

        String containerId = cluster.addAndStartContainer(extraAgent);
        assertNotNull("freshly started container is not found", DockerContainersUtil.getContainer(dockerClient, containerId));

        cluster.stop();
        assertNull("new container should be stopped too", DockerContainersUtil.getContainer(dockerClient, containerId));

    }

}
