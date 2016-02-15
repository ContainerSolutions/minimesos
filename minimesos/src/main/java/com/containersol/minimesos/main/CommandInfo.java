package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;

import java.io.PrintStream;

/**
 * Info command
 */
@Parameters(separators = "=", commandDescription = "Display cluster information")
public class CommandInfo implements Command {

    public static final String CLINAME = "info";

    private PrintStream output = System.out;

    public CommandInfo() {
    }

    public CommandInfo(PrintStream ps) {
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
            cluster.info(output);
        } else {
            output.println("Minimesos cluster is not running");
        }
    }
}
