package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ExposedPortsTest {

    private static final boolean EXPOSED_PORTS = true;

    protected static final String resources = MesosAgent.DEFAULT_PORT_RESOURCES + "; cpus(*):0.2; mem(*):256; disk(*):200";
    protected static final DockerClient dockerClient = DockerClientFactory.build();

    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder(dockerClient)
            .withZooKeeper()
            .withMaster(zooKeeper -> new MesosMasterExtended(dockerClient, zooKeeper, MesosMaster.MESOS_MASTER_IMAGE, MesosContainer.MESOS_IMAGE_TAG, new TreeMap<>(), EXPOSED_PORTS ))
            .withAgent(zooKeeper -> new MesosAgent(dockerClient, resources, 5051, zooKeeper, MesosAgent.MESOS_AGENT_IMAGE, MesosContainer.MESOS_IMAGE_TAG))
            .withMarathon(zooKeeper -> new Marathon(dockerClient, zooKeeper, true))
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);


    @Test
    public void testLoadCluster() {

        String clusterId = CLUSTER.getClusterId();
        MesosCluster cluster = MesosCluster.loadCluster(clusterId);

        assertTrue( "Deserialize cluster is expected to remember exposed ports setting", cluster.isExposedHostPorts() );
    }

}
