package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandUninstallTest {

    private ByteArrayOutputStream outputStream;

    private PrintStream ps;
    private Marathon marathon;
    private MesosCluster mesosCluster;
    private ClusterRepository repository;
    private CommandUninstall commandUninstall;

    @Before
    public void initTest() {
        outputStream = new ByteArrayOutputStream();
        ps = new PrintStream(outputStream, true);

        marathon = mock(Marathon.class);

        mesosCluster = mock(MesosCluster.class);
        when(mesosCluster.getMarathon()).thenReturn(marathon);

        repository = mock(ClusterRepository.class);
        when(repository.loadCluster(any(MesosClusterFactory.class))).thenReturn(mesosCluster);

        commandUninstall = new CommandUninstall(ps);
        commandUninstall.setRepository(repository);
        commandUninstall.setApp("app");
    }

    @Test
    public void execute() {
        doNothing().when(marathon).deleteApp("app");

        commandUninstall.execute();

        String result = outputStream.toString();
        assertEquals("Deleted app 'app'\n", result);
    }

    @Test
    public void execute_appDoesNotExist() {
        doThrow(new MinimesosException("App does not exist")).when(marathon).deleteApp("app");

        commandUninstall.execute();

        String result = outputStream.toString();
        assertEquals("", result);
    }
}
