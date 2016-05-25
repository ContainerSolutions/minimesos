package com.containersol.minimesos.main;

import java.io.PrintStream;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

/**
 * Parameters for the 'state' command
 */
@Parameters(separators = "=", commandDescription = "Display state.json file of a master or an agent")
public class CommandState implements Command {

    public static final String CLINAME = "state";

    @Parameter(names = "--agent", description = "Specify an agent to query, otherwise query a master")
    private String agent = "";

    private PrintStream output = System.out; //NOSONAR

    private ClusterRepository repository = new ClusterRepository();

    public CommandState() {
    }

    public CommandState(PrintStream ps) {
        this.output = ps;
    }

    @Override
    public void execute() {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());
        if (cluster != null) {
            cluster.state(output, agent);
        } else {
            output.println("Minimesos cluster is not running");
        }
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

}
