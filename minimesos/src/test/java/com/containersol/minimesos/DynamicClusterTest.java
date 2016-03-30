package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.MesosAgent;
import com.containersol.minimesos.mesos.MesosMaster;
import com.containersol.minimesos.mesos.ZooKeeper;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DynamicClusterTest {

    private static final boolean EXPOSED_PORTS = false;

    @Test
    public void noMarathonTest() {

        MesosMasterConfig masterConfig = new MesosMasterConfig();

        ClusterArchitecture config = new ClusterArchitecture.Builder()
                .withZooKeeper()
                .withMaster(zooKeeper -> new MesosMaster(zooKeeper, masterConfig))
                .withAgent(MesosAgent::new)
                .build();

        MesosCluster cluster = new MesosCluster(config);
        cluster.setExposedHostPorts(EXPOSED_PORTS);

        cluster.start();
        String clusterId = cluster.getClusterId();

        assertNotNull( "Cluster ID must be set", clusterId );

        // this should not throw any exceptions
        cluster.destroy();

    }

    @Test
    public void stopWithNewContainerTest() {

        MesosMasterConfig masterConfig = new MesosMasterConfig();

        ClusterArchitecture config = new ClusterArchitecture.Builder()
                .withZooKeeper()
                .withMaster(zooKeeper -> new MesosMaster(zooKeeper, masterConfig))
                .withAgent(MesosAgent::new)
                .build();

        MesosCluster cluster = new MesosCluster(config);
        cluster.setExposedHostPorts(EXPOSED_PORTS);
        cluster.start();

        ZooKeeper zooKeeper = cluster.getZkContainer();
        MesosAgent extraAgent = new MesosAgent(zooKeeper);

        String containerId = cluster.addAndStartContainer(extraAgent);
        assertNotNull("freshly started container is not found", DockerContainersUtil.getContainer(containerId));

        cluster.stop();
        assertNull("new container should be stopped too", DockerContainersUtil.getContainer(containerId));

    }

}
