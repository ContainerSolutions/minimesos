package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import org.junit.Test;

import static com.containersol.minimesos.mesos.MesosContainers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Mesos Arch test
 */
public class MesosArchitectureTest {
    @Test
    public void shouldAllowDefaultArchitecture() {
        MesosArchitecture.Builder builder = new MesosArchitecture.Builder();
        MesosArchitecture mesosArchitecture = builder.build();
        assertTrue(mesosArchitecture.getMesosContainers().getContainers().size() > 0);
        assertTrue(mesosArchitecture.getMesosContainers().getContainers().stream().filter(Filter.zooKeeper()).findFirst().isPresent());
        assertTrue(mesosArchitecture.getMesosContainers().getContainers().stream().filter(Filter.mesosMaster()).findFirst().isPresent());
        assertTrue(mesosArchitecture.getMesosContainers().getContainers().stream().filter(Filter.mesosSlave()).findFirst().isPresent());
    }

    @Test( expected = MesosArchitecture.MesosArchitectureException.class)
    public void shouldErrorIfNoZooKeeperIsPresentAndMesosAdded() {
        new MesosArchitecture.Builder().withMaster();
    }

    @Test
    public void shouldBeAbleToAddMaster() {
        MesosArchitecture.Builder builder = new MesosArchitecture.Builder().withZooKeeper().withMaster();
        assertEquals(minimumViableClusterSize(), builder.build().getMesosContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddCustomMaster() {
        MesosMaster mock = mock(MesosMaster.class);
        MesosArchitecture.Builder builder = new MesosArchitecture.Builder().withZooKeeper().withMaster(mock);
        assertEquals(minimumViableClusterSize(), builder.build().getMesosContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddSlave() {
        MesosArchitecture.Builder builder = new MesosArchitecture.Builder().withZooKeeper().withSlave();
        assertEquals(minimumViableClusterSize(), builder.build().getMesosContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddCustomSlave() {
        MesosSlave mock = mock(MesosSlave.class);
        MesosArchitecture.Builder builder = new MesosArchitecture.Builder().withZooKeeper().withSlave(mock);
        assertEquals(minimumViableClusterSize(), builder.build().getMesosContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddContainer() {
        AbstractContainer mock = mock(AbstractContainer.class);
        MesosArchitecture.Builder builder = new MesosArchitecture.Builder().withContainer(mock);
        assertEquals(minimumViableClusterSize() + 1, builder.build().getMesosContainers().getContainers().size());
    }

    private int minimumViableClusterSize() {
        return new MesosArchitecture.Builder().build().getMesosContainers().getContainers().size();
    }
}