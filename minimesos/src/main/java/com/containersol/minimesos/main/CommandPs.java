package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
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
public class CommandPs implements Command {

    private ClusterRepository repository = new ClusterRepository();

    private PrintStream output = System.out;

    public CommandPs(PrintStream output) {
        this.output = output;
    }

    public CommandPs() {
        this.output = System.out;
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
    public void execute() throws MinimesosException {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());

        if (cluster == null) {
            output.println("Minimesos cluster is not running");
            return;
        }

        State state = cluster.getMaster().getState();
        for (Framework framework : state.getFrameworks()) {
            for (Task task : framework.getTasks()) {
                output.println(framework.getName() + "\t" + task.getName() + "\t" + task.getState());
            }
        }
    }

    public void setRepository(ClusterRepository repository) {
        this.repository = repository;
    }
}
