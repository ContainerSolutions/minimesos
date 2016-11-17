package com.containersol.minimesos.factory;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MesosClusterContainersFactoryTest {

    @Test
    public void testCreateMesosCluster() throws FileNotFoundException {
        MesosCluster mesosCluster = new MesosClusterContainersFactory().createMesosCluster(new FileInputStream("src/test/resources/configFiles/minimesosFile-mesosClusterTest"));
        assertEquals(3 , mesosCluster.getAgents().size());
        assertNotNull(mesosCluster.getZooKeeper());
        assertNotNull(mesosCluster.getMaster());
    }

}
