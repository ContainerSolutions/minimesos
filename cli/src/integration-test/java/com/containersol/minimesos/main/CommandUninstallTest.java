package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import mesosphere.marathon.client.model.v2.Result;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
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

        marathon = Mockito.mock(Marathon.class);

        mesosCluster = Mockito.mock(MesosCluster.class);
        when(mesosCluster.getMarathon()).thenReturn(marathon);

        repository = Mockito.mock(ClusterRepository.class);
        when(repository.loadCluster(Matchers.any(MesosClusterFactory.class))).thenReturn(mesosCluster);

        commandUninstall = new CommandUninstall(ps);
        commandUninstall.setRepository(repository);
    }

    @Test
    public void execute_app() throws UnsupportedEncodingException {
        // Given
        commandUninstall.setApp("/app");
        when(marathon.deleteApp("/app")).thenReturn(new Result());

        // When
        commandUninstall.execute();

        // Then
        String string = outputStream.toString("UTF-8");
        assertEquals("Deleted app '/app'\n", string);
    }

    @Test
    public void execute_group() throws UnsupportedEncodingException {
        // Given
        commandUninstall.setGroup("/group");
        when(marathon.deleteGroup("/group")).thenReturn(new Result());

        // When
        commandUninstall.execute();

        // Then
        String string = outputStream.toString("UTF-8");
        assertEquals("Deleted group '/group'\n", string);
    }

    @Test
    public void execute_appAndGroup() throws UnsupportedEncodingException {
        // Given
        commandUninstall.setGroup("/group1");
        commandUninstall.setApp("/app2");

        // When
        commandUninstall.execute();

        // Then
        String string = outputStream.toString("UTF-8");
        assertEquals("Please specify --app or --group to uninstall an app or group\n", string);
    }

    @Test
    public void execute_appDoesNotExist() throws UnsupportedEncodingException {
        Mockito.doThrow(new MinimesosException("App does not exist")).when(marathon).deleteApp("app");

        commandUninstall.execute();

        String result = outputStream.toString("UTF-8");
        assertEquals("Please specify --app or --group to uninstall an app or group\n", result);
    }
}
