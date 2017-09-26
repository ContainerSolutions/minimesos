package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import com.containersol.minimesos.state.Executor;
import com.containersol.minimesos.state.Framework;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.state.Task;
import org.apache.http.client.utils.URIBuilder;

import java.io.PrintStream;
import java.net.URISyntaxException;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@Parameters(separators = "=", commandDescription = "Fetches the logs of the specified task")
public class CommandLogs implements Command {

    private PrintStream output = System.out; // NOSONAR

    private ClusterRepository repository = new ClusterRepository();

    @Parameter(names = "--task", description = "Substring of a task ID", required = true)
    String taskId = null;

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
        if (taskId == null) {
            return;
        }

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

        State agentState = agent.getState();
        Executor executor = findExecutor(agentState, task.getId());

        if (executor == null) {
            output.println(String.format("Cannot find executor: '%s'", task.getId()));
            return;
        }

        String path = executor.getDirectory();

        URIBuilder uriBuilder = new URIBuilder(agent.getServiceUrl())
            .setPath("/files/download")
            .addParameter("path", path + "/stderr");
        try {
            output.println(uriBuilder.build().toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void setRepository(ClusterRepository repository) {
        this.repository = repository;
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

    private Executor findExecutor(State agentState, String taskId) {
        for (Framework framework : agentState.getFrameworks()) {
            for (Executor executor : framework.getExecutors()) {
                if (executor.getId().equals(taskId)) {
                    return  executor;
                }
            }
        }
        return null;
    }

}
