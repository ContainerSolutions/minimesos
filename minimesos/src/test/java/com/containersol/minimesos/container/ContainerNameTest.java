package com.containersol.minimesos.container;

import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersol.minimesos.mesos.MesosAgent;
import com.github.dockerjava.api.DockerClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContainerNameTest {

    protected static final DockerClient dockerClient = DockerClientFactory.build();

    @Test
    public void testBelongsToCluster() throws Exception {
        MesosAgent agent = new MesosAgent(dockerClient, "CLUSTERID", "UUID", "CONTAINERID");
        String containerName = ContainerName.get(agent);

        assertTrue(ContainerName.hasRoleInCluster(containerName, "CLUSTERID", agent.getRole()));
        assertTrue(ContainerName.belongsToCluster(containerName, "CLUSTERID"));
    }

    @Test
    public void testWrongCluster() throws Exception {
        MesosAgent agent = new MesosAgent(dockerClient, "CLUSTERID", "UUID", "CONTAINERID");
        String containerName = ContainerName.get(agent);

        assertFalse(ContainerName.hasRoleInCluster(containerName, "XXXXXX", agent.getRole()));
        assertFalse(ContainerName.belongsToCluster(containerName, "XXXXXX"));
    }

    @Test
    public void testWrongRole() throws Exception {
        MesosAgent agent = new MesosAgent(dockerClient, "CLUSTERID", "UUID", "CONTAINERID");
        String containerName = ContainerName.get(agent);

        assertFalse(ContainerName.hasRoleInCluster(containerName, "CLUSTERID", "XXXXXX"));
        assertTrue(ContainerName.belongsToCluster(containerName, "CLUSTERID"));
    }

    @Test
    public void testSimpleContainerName() {
        String[] names = new String[1];
        names[0] = "/minimesos-agent";

        assertEquals("minimesos-agent", ContainerName.getFromDockerNames(names));
    }

    @Test
    public void testLinkedContainerNames() {
        String[] names = new String[4];
        names[0] = "/minimesos-agent0/minimesos-zookeeper";
        names[1] = "/minimesos-agent1/minimesos-zookeeper";
        names[2] = "/minimesos-agent2/minimesos-zookeeper";
        names[3] = "/minimesos-zookeeper";

        assertEquals("minimesos-zookeeper", ContainerName.getFromDockerNames(names));
    }

}