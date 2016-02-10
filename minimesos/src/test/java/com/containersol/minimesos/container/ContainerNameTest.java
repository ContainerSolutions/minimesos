package com.containersol.minimesos.container;

import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersol.minimesos.mesos.MesosSlave;
import com.github.dockerjava.api.DockerClient;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContainerNameTest {

    protected static final DockerClient dockerClient = DockerClientFactory.build();

    @Test
    public void testBelongsToCluster() throws Exception {

        MesosSlave slave = new MesosSlave(dockerClient, "CLUSTERID", "UUID", "CONTAINERID");
        String containerName = ContainerName.get( slave );

        assertTrue( ContainerName.hasRoleInCluster(containerName, "CLUSTERID", slave.getRole() ));
        assertTrue( ContainerName.belongsToCluster(containerName, "CLUSTERID" ) );

    }

    @Test
    public void testWrongCluster() throws Exception {

        MesosSlave slave = new MesosSlave(dockerClient, "CLUSTERID", "UUID", "CONTAINERID");
        String containerName = ContainerName.get( slave );

        assertFalse( ContainerName.hasRoleInCluster(containerName, "XXXXXX", slave.getRole() ));
        assertFalse( ContainerName.belongsToCluster(containerName, "XXXXXX" ) );

    }

    @Test
    public void testWrongRole() throws Exception {

        MesosSlave slave = new MesosSlave(dockerClient, "CLUSTERID", "UUID", "CONTAINERID");
        String containerName = ContainerName.get( slave );

        assertFalse( ContainerName.hasRoleInCluster(containerName, "CLUSTERID", "XXXXXX" ));
        assertTrue( ContainerName.belongsToCluster(containerName, "CLUSTERID" ) );

    }

    @Test
    public void testSimpleContainerName() {

        String[] names = new String[1];
        names[0] = "/minimesos-agent";

        assertEquals( "minimesos-agent", ContainerName.getFromDockerNames(names));

    }

    @Test
    public void testLinkedContainerNames() {

        String[] names = new String[4];
        names[0] = "/minimesos-agent0/minimesos-zookeeper";
        names[1] = "/minimesos-agent1/minimesos-zookeeper";
        names[2] = "/minimesos-agent2/minimesos-zookeeper";
        names[3] = "/minimesos-zookeeper";

        assertEquals( "minimesos-zookeeper", ContainerName.getFromDockerNames(names));

    }

}