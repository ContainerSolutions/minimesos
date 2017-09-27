package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import com.containersol.minimesos.state.Executor;
import com.containersol.minimesos.state.Framework;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.state.Task;
import com.containersol.minimesos.util.Downloader;
import org.apache.http.client.utils.URIBuilder;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isBlank;

@Parameters(separators = "=", commandDescription = "Fetches the stdout logs of the specified task")
public class CommandLogs implements Command {

    private PrintStream output = System.out; // NOSONAR

    private ClusterRepository repository = new ClusterRepository();

    private Downloader downloader = new Downloader();

    @Parameter(names = "--task", description = "Substring of a task ID", required = true)
    String taskId = null;

    @Parameter(names = "--stderr", description = "Fetch the stderr logs instead of stdout")
    Boolean stderr = false;

    public CommandLogs(PrintStream output) {
        this.output = output;
    }

    public CommandLogs() {
        //NOSONAR
    }

    @Override
    public boolean validateParameters() {
        return isNotBlank(taskId);
    }

    @Override
    public String getName() {
        return "logs";
    }

    @Override
    public void execute() {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());
        if (cluster == null) {
            output.println("Minimesos cluster is not running");
            return;
        }

        State masterState = cluster.getMaster().getState();
        Task task = findTask(masterState, taskId);
        if (task == null) {
            output.println(String.format("Cannot find task: '%s'", taskId));
            return;
        }

        MesosAgent agent = findAgent(cluster, task.getSlaveId());
        if (agent == null) {
            output.println(String.format("Cannot find agent: '%s'", task.getSlaveId()));
            return;
        }


        String filename = stderr ? "stderr" : "stdout";
        output.println(String.format("[minimesos] Fetching '%s' of task '%s'\n", filename, task.getId()));
        URI fileUrl = getFileUrl(agent, task, filename);
        String content = downloader.getFileContentAsString(fileUrl.toString());
        output.println(content);
    }

    public void setRepository(ClusterRepository repository) {
        this.repository = repository;
    }

    void setDownloader(Downloader downloader) {
        this.downloader = downloader;
    }

    private Task findTask(State state, String taskId) {
        for (Framework framework : state.getFrameworks()) {
            for (Task task: framework.getTasks()) {
                if (task.getId().contains(taskId)) {
                    return task;
                }
            }
        }
        return null;
    }

    private MesosAgent findAgent(MesosCluster cluster, String slaveId) {
        for (MesosAgent agent : cluster.getAgents()) {
            State agentState = agent.getState();
            if (agentState.getId().equals(slaveId)) {
                return agent;
            }
        }
        return null;
    }

    private URI getFileUrl(MesosAgent agent, Task task, String filename) throws MinimesosException {
        Executor executor = findExecutor(agent, task);
        if (executor == null) {
            throw new MinimesosException(String.format("Cannot find executor: '%s'", taskId));
        }
        String path = executor.getDirectory();
        URIBuilder uriBuilder = new URIBuilder(agent.getServiceUrl())
            .setPath("/files/download")
            .addParameter("path", path + "/" + filename);
        URI sandboxUrl = null;
        try {
            sandboxUrl = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new MinimesosException(e.getMessage());
        }
        return sandboxUrl;
    }

    private Executor findExecutor(MesosAgent agent, Task task) {
        String executorId = task.getExecutorId();
        if (isBlank(executorId)) { // if executorId is empty, try with the taskId
            executorId = task.getId();
        }
        for (Framework framework : agent.getState().getFrameworks()) {
            if (framework.getId().equals(task.getFrameworkId())) {
                for (Executor executor : framework.getExecutors()) {
                    if (executor.getId().equals(executorId)) {
                        return executor;
                    }
                }
            }
        }
        return null;
    }

}
