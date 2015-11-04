package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import org.junit.Test;

import static com.containersol.minimesos.mesos.MesosContainers.*;
import static org.mockito.Mockito.*;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests
 */
public class MesosContainersTest {
    @Test
    public void shouldEmptyStart() {
        assertTrue(new MesosContainers().getContainers().isEmpty());
    }

    @Test
    public void shouldAllowInjection() {
        List dummyList = mock(List.class);
        assertEquals(dummyList, new MesosContainers(dummyList).getContainers());
    }

    @Test
    public void shouldFilterZooKeeper() {
        ZooKeeper mock = mock(ZooKeeper.class);
        AbstractContainer abstractContainer = mock(AbstractContainer.class);
        MesosContainers mesosContainers = new MesosContainers();
        mesosContainers.add(mock).add(abstractContainer);

        assertTrue(mesosContainers.isPresent(Filter.zooKeeper()));
    }

    @Test
    public void shouldFilterMesosMaster() {
        MesosMaster mock = mock(MesosMaster.class);
        AbstractContainer abstractContainer = mock(AbstractContainer.class);
        MesosContainers mesosContainers = new MesosContainers();
        mesosContainers.add(mock).add(abstractContainer);

        assertTrue(mesosContainers.isPresent(Filter.mesosMaster()));
    }

    @Test
    public void shouldFilterMesosSlave() {
        MesosSlave mock = mock(MesosSlave.class);
        AbstractContainer abstractContainer = mock(AbstractContainer.class);
        MesosContainers mesosContainers = new MesosContainers();
        mesosContainers.add(mock).add(abstractContainer);

        assertTrue(mesosContainers.isPresent(Filter.mesosSlave()));
    }
}