package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

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
        Mockito.when(mesosCluster.getMarathon()).thenReturn(marathon);

        repository = Mockito.mock(ClusterRepository.class);
        Mockito.when(repository.loadCluster(Matchers.any(MesosClusterFactory.class))).thenReturn(mesosCluster);

        commandUninstall = new CommandUninstall(ps);
        commandUninstall.setRepository(repository);
        commandUninstall.setApp("app");
    }

    @Test
    public void execute() throws UnsupportedEncodingException {
        Mockito.doNothing().when(marathon).deleteApp("app");

        commandUninstall.execute();

        String result = outputStream.toString("UTF-8");
        Assert.assertEquals("Deleted app 'app'\n", result);
    }

    @Test
    public void execute_appDoesNotExist() throws UnsupportedEncodingException {
        Mockito.doThrow(new MinimesosException("App does not exist")).when(marathon).deleteApp("app");

        commandUninstall.execute();

        String result = outputStream.toString("UTF-8");
        Assert.assertEquals("", result);
    }
}
