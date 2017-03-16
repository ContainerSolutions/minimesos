package com.containersol.minimesos.main;

import java.io.PrintStream;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.MesosClusterDockerFactory;

/**
 * Parameters for the 'state' command
 */
@Parameters(separators = "=", commandDescription = "Display the master's state.json file")
public class CommandState implements Command {

    private static final String CLINAME = "state";

    private PrintStream output = System.out; //NOSONAR

    CommandState() { //NOSONAR
    }

    CommandState(PrintStream ps) {
        this.output = ps;
    }

    MesosClusterDockerFactory factory = new MesosClusterDockerFactory();

    @Override
    public void execute() {
        MesosCluster cluster = factory.retrieveMesosCluster();
        if (cluster != null) {
            cluster.state(output);
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
