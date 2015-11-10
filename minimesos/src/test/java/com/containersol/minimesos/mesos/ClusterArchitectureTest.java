package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import org.junit.Test;

import static com.containersol.minimesos.mesos.ClusterContainers.Filter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Mesos Arch test
 */
public class ClusterArchitectureTest {
    @Test
    public void shouldAllowDefaultArchitecture() {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder();
        ClusterArchitecture clusterArchitecture = builder.build();
        assertTrue(clusterArchitecture.getClusterContainers().getContainers().size() > 0);
        assertTrue(clusterArchitecture.getClusterContainers().getContainers().stream().filter(Filter.zooKeeper()).findFirst().isPresent());
        assertTrue(clusterArchitecture.getClusterContainers().getContainers().stream().filter(Filter.mesosMaster()).findFirst().isPresent());
        assertTrue(clusterArchitecture.getClusterContainers().getContainers().stream().filter(Filter.mesosSlave()).findFirst().isPresent());
    }

    @Test( expected = ClusterArchitecture.MesosArchitectureException.class)
    public void shouldErrorIfNoZooKeeperIsPresentAndMasterAdded() {
        new ClusterArchitecture.Builder().withMaster();
    }

    @Test( expected = ClusterArchitecture.MesosArchitectureException.class)
    public void shouldErrorIfNoZooKeeperIsPresentAndSlaveAdded() {
        new ClusterArchitecture.Builder().withSlave();
    }

    @Test
    public void shouldBeAbleToAddCustomZooKeeper() {
        ZooKeeper mock = mock(ZooKeeper.class);
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper(mock);
        assertEquals(minimumViableClusterSize(), builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddMaster() {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper().withMaster();
        assertEquals(minimumViableClusterSize(), builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddCustomMaster() {
        MesosMaster mock = mock(MesosMaster.class);
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper().withMaster(zk -> mock);
        assertEquals(minimumViableClusterSize(), builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddSlave() {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper().withSlave();
        assertEquals(minimumViableClusterSize(), builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddCustomSlave() {
        MesosSlave mock = mock(MesosSlave.class);
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper().withSlave(zk -> mock);
        assertEquals(minimumViableClusterSize(), builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddContainer() {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withContainer(mock(AbstractContainer.class));
        assertEquals(minimumViableClusterSize() + 1, builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void plainContainerOrderingShouldNotMatter() {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withContainer(mock(AbstractContainer.class)).withZooKeeper().withContainer(mock(AbstractContainer.class)).withMaster();
        assertEquals(minimumViableClusterSize() + 2, builder.build().getClusterContainers().getContainers().size());
    }

    private int minimumViableClusterSize() {
        return new ClusterArchitecture.Builder().build().getClusterContainers().getContainers().size();
    }
}