package com.containersol.minimesos.container;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.MesosAgentContainer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContainerNameTest {

    private MesosCluster cluster;
    private String clusterId;

    @Before
    public void before() {

        ClusterArchitecture architecture = new ClusterArchitecture();
        cluster = new MesosCluster(architecture.getClusterConfig(), architecture.getClusterContainers().getContainers());

        clusterId = cluster.getClusterId();
    }

    @Test
    public void testBelongsToCluster() throws Exception {
        MesosAgentContainer agent = new MesosAgentContainer(cluster, "UUID", "CONTAINERID");
        String containerName = ContainerName.get(agent);

        assertTrue(ContainerName.hasRoleInCluster(containerName, clusterId, agent.getRole()));
        assertTrue(ContainerName.belongsToCluster(containerName, clusterId));
    }

    @Test
    public void testWrongCluster() throws Exception {
        MesosAgentContainer agent = new MesosAgentContainer(cluster, "UUID", "CONTAINERID");
        String containerName = ContainerName.get(agent);

        assertFalse(ContainerName.hasRoleInCluster(containerName, "XXXXXX", agent.getRole()));
        assertFalse(ContainerName.belongsToCluster(containerName, "XXXXXX"));
    }

    @Test
    public void testWrongRole() throws Exception {
        MesosAgentContainer agent = new MesosAgentContainer(cluster, "UUID", "CONTAINERID");
        String containerName = ContainerName.get(agent);

        assertFalse(ContainerName.hasRoleInCluster(containerName, clusterId, "XXXXXX"));
        assertTrue(ContainerName.belongsToCluster(containerName, clusterId));
    }

    @Test
    public void testSimpleContainerName() {
        String[] names = new String[1];
        names[0] = "/minimesos-agent";

        assertEquals("minimesos-agent", ContainerName.getFromDockerNames(names));
    }

}