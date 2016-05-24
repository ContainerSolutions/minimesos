package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CommandUpTest {

    private CommandUp commandUp;

    private ArgumentCaptor<ClusterConfig> capturedClusterConfig;

    private MesosCluster mesosCluster;

    @Before
    public void before() {
        MesosClusterContainersFactory mesosClusterFactory = mock(MesosClusterContainersFactory.class);
        mesosCluster = mock(MesosCluster.class);
        when(mesosCluster.getClusterId()).thenReturn("123456");

        capturedClusterConfig = ArgumentCaptor.forClass(ClusterConfig.class);
        when(mesosClusterFactory.createMesosCluster(capturedClusterConfig.capture())).thenReturn(mesosCluster);

        commandUp = new CommandUp();
        commandUp.setMesosClusterFactory(mesosClusterFactory);
    }

    @Test(expected = MinimesosException.class)
    public void testExecute_missingMinimesosFile() throws IOException {
        commandUp.execute();
    }

    @Test(expected = MinimesosException.class)
    public void testExecute_invalidMinimesosFile() throws IOException {
        commandUp.setClusterConfigPath("src/test/resources/configFiles/invalid-minimesosFile");
        commandUp.execute();
    }

    @Test
    public void testBasicClusterConfig() throws IOException {
        commandUp.setClusterConfigPath("src/test/resources/clusterconfig/basic.groovy");
        commandUp.execute();

        verify(mesosCluster).start();
    }

    @Test
    public void testExecute_mapPortsToHost() {
        commandUp.setClusterConfigPath("src/test/resources/configFiles/minimal-minimesosFile");
        commandUp.setMapPortsToHost(true);
        commandUp.execute();

        assertTrue("Map ports to host from configuration is expected to remain", capturedClusterConfig.getValue().isMapPortsToHost());
    }

}
