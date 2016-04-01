package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.AbstractContainer;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosMaster;
import com.containersol.minimesos.cluster.ZooKeeper;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.containersol.minimesos.mesos.ClusterContainers.Filter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests
 */
public class ClusterContainersTest {
    @Test
    public void shouldEmptyStart() {
        assertTrue(new ClusterContainers().getContainers().isEmpty());
    }

    @Test
    public void shouldAllowInjection() {
        List<AbstractContainer> dummyList = new ArrayList<>();
        assertEquals(dummyList, new ClusterContainers(dummyList).getContainers());
    }

    @Test
    public void shouldFilterZooKeeper() {
        ZooKeeper mock = mock(ZooKeeper.class);
        AbstractContainer abstractContainer = mock(AbstractContainer.class);
        ClusterContainers clusterContainers = new ClusterContainers();
        clusterContainers.add(mock).add(abstractContainer);

        assertTrue(clusterContainers.isPresent(Filter.zooKeeper()));
    }

    @Test
    public void shouldFilterMesosMaster() {
        MesosMaster mock = mock(MesosMaster.class);
        AbstractContainer abstractContainer = mock(AbstractContainer.class);
        ClusterContainers clusterContainers = new ClusterContainers();
        clusterContainers.add(mock).add(abstractContainer);

        assertTrue(clusterContainers.isPresent(Filter.mesosMaster()));
    }

    @Test
    public void shouldFilterMesosAgent() {
        MesosAgent mock = mock(MesosAgent.class);
        AbstractContainer abstractContainer = mock(AbstractContainer.class);
        ClusterContainers clusterContainers = new ClusterContainers();
        clusterContainers.add(mock).add(abstractContainer);

        assertTrue(clusterContainers.isPresent(Filter.mesosAgent()));
    }
}