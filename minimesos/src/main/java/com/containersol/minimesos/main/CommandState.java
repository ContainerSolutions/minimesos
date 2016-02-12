package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

/**
 * Parameters for the 'state' command
 *
 */
@Parameters(separators = "=", commandDescription = "Display state.json file of a master or an agent")
public class CommandState implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final String CLINAME = "state";

    @Parameter(names = "--agent", description = "Specify an agent to query, otherwise query a master")
    private String agent = "";

    private PrintStream output = System.out;

    public CommandState() {
    }

    public CommandState(PrintStream ps) {
        this.output = ps;
    }

    @Override
    public boolean isExposedHostPorts() {
        return false;
    }

    @Override
    public boolean getStartConsul() {
        return false;
    }

    public void execute() {
        MesosCluster cluster = ClusterRepository.loadCluster();
        if (cluster != null) {
            cluster.state( output, agent);
        } else {
            output.println("Minimesos cluster is not running");
        }
    }

}
