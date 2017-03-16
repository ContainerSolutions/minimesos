package com.containersol.minimesos.factory;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.docker.MesosClusterDockerFactory;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MesosClusterContainersFactoryTest {

    @Test
    public void testCreateMesosCluster() throws FileNotFoundException {
        MesosCluster mesosCluster = new MesosClusterDockerFactory().createMesosCluster(new FileInputStream("src/test/resources/configFiles/minimesosFile-mesosClusterTest"));
        assertEquals(3 , mesosCluster.getAgents().size());
        assertNotNull(mesosCluster.getZooKeeper());
        assertNotNull(mesosCluster.getMaster());
    }

    @Test(expected = MinimesosException.class)
    public void testLoadCluster_noContainersFound() {
        MesosClusterFactory factory = new MesosClusterDockerFactory();
        factory.retrieveMesosCluster("nonexistent");
    }

}
