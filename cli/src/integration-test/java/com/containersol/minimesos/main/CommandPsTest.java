package com.containersol.minimesos.main;

import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.mesos.MesosMasterContainer;
import com.containersol.minimesos.state.Framework;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.state.Task;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandPsTest {

    private static final String FORMAT = "%-20s %-20s %s\n";
    private static final Object[] COLUMNS = { "FRAMEWORK", "TASK", "STATE" };
    private static final Object[] VALUES = {"marathon", "weave-scope", "TASK_RUNNING" };

    private ByteArrayOutputStream outputStream;

    private PrintStream ps;

    @Before
    public void initTest() {
        outputStream = new ByteArrayOutputStream();
        ps = new PrintStream(outputStream, true);
    }

    @Test
    public void execute() throws UnsupportedEncodingException {
        State state = new State();
        Framework marathon = new Framework();
        marathon.setName("marathon");

        Task task = new Task();
        task.setName("weave-scope");
        task.setState("TASK_RUNNING");

        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(task);

        marathon.setTasks(tasks);

        ArrayList<Framework> frameworks = new ArrayList<>();
        frameworks.add(marathon);
        state.setFrameworks(frameworks);

        MesosMasterContainer master = mock(MesosMasterContainer.class);
        when(master.getState()).thenReturn(state);

        MesosCluster mesosCluster = mock(MesosCluster.class);
        when(mesosCluster.getMaster()).thenReturn(master);

        ClusterRepository repository = mock(ClusterRepository.class);
        when(repository.loadCluster(any(MesosClusterFactory.class))).thenReturn(mesosCluster);

        CommandPs commandPs = new CommandPs(ps);
        commandPs.setRepository(repository);

        commandPs.execute();

        String result = outputStream.toString("UTF-8");
        assertEquals(String.format(FORMAT, COLUMNS) + String.format(FORMAT, VALUES), result);
    }

}
