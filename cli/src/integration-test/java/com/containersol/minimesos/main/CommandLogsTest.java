package com.containersol.minimesos.main;

import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.mesos.MesosAgentContainer;
import com.containersol.minimesos.mesos.MesosMasterContainer;
import com.containersol.minimesos.state.*;
import com.containersol.minimesos.util.Downloader;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandLogsTest {

    private final String slaveId = "de09c926-7be6-47c6-ab9a-6d1a2b32b642-S0";
    private final String taskId = "http-ports-static-assigned-to-31002.0f732069-a1f2-11e7-97e0-0242ac110006";
    private final String workDir = "/var/lib/mesos/agent-3039938407";
    private final String frameworkId = "de09c926-7be6-47c6-ab9a-6d1a2b32b642-0000";
    private final String runId = "fd7f7182-5ee8-47fc-a1c4-6928aeb1f769";
    private final String agentServiceURL = "http://172.17.0.7:5051";

    private final String PATH_FORMAT = "%s/slaves/%s/frameworks/%s/executors/%s/runs/%s";
    private final String executorDirectory = String.format(PATH_FORMAT, workDir, slaveId, frameworkId, taskId, runId);

    private final String STDOUT_URL = "http://172.17.0.7:5051" +
        "/files/download?path=" +
        "%2Fvar%2Flib%2Fmesos" +
        "%2Fagent-3039938407" +
        "%2Fslaves%2Fde09c926-7be6-47c6-ab9a-6d1a2b32b642-S0" +
        "%2Fframeworks%2Fde09c926-7be6-47c6-ab9a-6d1a2b32b642-0000" +
        "%2Fexecutors%2Fhttp-ports-static-assigned-to-31002.0f732069-a1f2-11e7-97e0-0242ac110006" +
        "%2Fruns%2Ffd7f7182-5ee8-47fc-a1c4-6928aeb1f769" +
        "%2Fstdout";

    private final String STDERR_URL = "http://172.17.0.7:5051" +
        "/files/download?path=" +
        "%2Fvar%2Flib%2Fmesos" +
        "%2Fagent-3039938407" +
        "%2Fslaves%2Fde09c926-7be6-47c6-ab9a-6d1a2b32b642-S0" +
        "%2Fframeworks%2Fde09c926-7be6-47c6-ab9a-6d1a2b32b642-0000" +
        "%2Fexecutors%2Fhttp-ports-static-assigned-to-31002.0f732069-a1f2-11e7-97e0-0242ac110006" +
        "%2Fruns%2Ffd7f7182-5ee8-47fc-a1c4-6928aeb1f769" +
        "%2Fstderr";

    private ByteArrayOutputStream outputStream;

    private PrintStream ps;

    private ClusterRepository repository;

    private Downloader downloader;

    private State masterState;

    private State agentState;

    @Before
    public void initTest() throws URISyntaxException {
        outputStream = new ByteArrayOutputStream();
        ps = new PrintStream(outputStream, true);

        masterState = generateMasterState();
        agentState = generateAgentState();

        MesosMasterContainer master = mock(MesosMasterContainer.class);
        when(master.getState()).thenReturn(masterState);

        MesosAgentContainer agent = mock(MesosAgentContainer.class);
        when(agent.getState()).thenReturn(agentState);
        when(agent.getServiceUrl()).thenReturn(new URI(agentServiceURL));

        MesosCluster mesosCluster = mock(MesosCluster.class);
        when(mesosCluster.getMaster()).thenReturn(master);
        when(mesosCluster.getAgents()).thenReturn(Collections.singletonList(agent));

        repository = mock(ClusterRepository.class);
        when(repository.loadCluster(any(MesosClusterFactory.class))).thenReturn(mesosCluster);

        downloader = mock(Downloader.class);
        when(downloader.getFileContentAsString(STDOUT_URL)).thenReturn("stdout file content");
        when(downloader.getFileContentAsString(STDERR_URL)).thenReturn("stderr file content");
    }

    @Test
    public void TestStdout() throws UnsupportedEncodingException, URISyntaxException {
        // Given
        CommandLogs commandLogs = new CommandLogs(ps);
        commandLogs.setRepository(repository);
        commandLogs.setDownloader(downloader);
        commandLogs.taskId = taskId;

        // When
        commandLogs.execute();

        // Then
        String result = outputStream.toString("UTF-8");
        assertEquals(
            "[minimesos] Fetching 'stdout' of task 'http-ports-static-assigned-to-31002.0f732069-a1f2-11e7-97e0-0242ac110006'\n\n" +
            "stdout file content\n",
            result);

    }

    @Test
    public void TestUnexistingTask() throws UnsupportedEncodingException, URISyntaxException {
        // Given
        CommandLogs commandLogs = new CommandLogs(ps);
        commandLogs.setRepository(repository);
        commandLogs.setDownloader(downloader);
        commandLogs.taskId = "doesn't exist";

        // When
        commandLogs.execute();

        // Then
        String result = outputStream.toString("UTF-8");
        assertEquals("Cannot find task: 'doesn't exist'\n", result);

    }

    @Test
    public void TestStderr() throws UnsupportedEncodingException, URISyntaxException {
        // Given
        CommandLogs commandLogs = new CommandLogs(ps);
        commandLogs.setRepository(repository);
        commandLogs.setDownloader(downloader);
        commandLogs.taskId = taskId;
        commandLogs.stderr = true;

        // When
        commandLogs.execute();

        // Then
        String result = outputStream.toString("UTF-8");
        assertEquals(
            "[minimesos] Fetching 'stderr' of task 'http-ports-static-assigned-to-31002.0f732069-a1f2-11e7-97e0-0242ac110006'\n\n" +
                "stderr file content\n",
            result);
    }

    private State generateAgentState() {
        State state = new State();
        state.setId(slaveId);

        Framework marathon = new Framework();
        marathon.setName("marathon");
        marathon.setId(frameworkId);

        Executor executor = new Executor();
        executor.setDirectory(executorDirectory);
        executor.setId(taskId);
        ArrayList<Executor> executors = new ArrayList<>();
        executors.add(executor);
        marathon.setExecutors(executors);

        ArrayList<Framework> frameworks = new ArrayList<>();
        frameworks.add(marathon);
        state.setFrameworks(frameworks);

        return state;
    }

    private State generateMasterState() {
        State state = new State();
        Framework marathon = new Framework();
        marathon.setName("marathon");

        Task task = new Task();
        task.setName("weave-scope");
        task.setState("TASK_RUNNING");
        task.setId(taskId);
        task.setSlaveId(slaveId);
        task.setFrameworkId(frameworkId);
        task.setExecutorId("");

        Port port = new Port();
        port.setNumber(4040);

        Ports ports = new Ports();
        ports.setPorts(singletonList(port));

        Discovery discovery = new Discovery();
        discovery.setPorts(ports);

        task.setDiscovery(discovery);

        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(task);

        marathon.setTasks(tasks);

        ArrayList<Framework> frameworks = new ArrayList<>();
        frameworks.add(marathon);
        state.setFrameworks(frameworks);

        return state;
    }
}
