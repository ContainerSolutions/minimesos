package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import com.containersol.minimesos.state.Framework;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.state.Task;

import java.io.PrintStream;

/**
 * Lists tasks on the cluster
 */
@Parameters(separators = "=", commandDescription = "List running tasks")
public class CommandPs implements Command {

    private static final String FORMAT = "%-20s %-20s %s\n";
    private static final Object[] COLUMNS = { "FRAMEWORK", "TASK", "STATE" };

    private ClusterRepository repository = new ClusterRepository();

    private PrintStream output = System.out; // NOSONAR

    public CommandPs(PrintStream output) {
        this.output = output;
    }

    public CommandPs() {
        // NOSONAR
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return "ps";
    }

    @Override
    public void execute() {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());

        if (cluster == null) {
            output.println("Minimesos cluster is not running");
            return;
        }

        output.printf(FORMAT, COLUMNS);
        State state = cluster.getMaster().getState();
        for (Framework framework : state.getFrameworks()) {
            for (Task task : framework.getTasks()) {
                output.printf(FORMAT, framework.getName(), task.getName(), task.getState());
            }
        }
    }

    public void setRepository(ClusterRepository repository) {
        this.repository = repository;
    }
}
