package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MesosCluster;

/**
 * Parameters for the 'destroy' command.
 */
@Parameters(separators = "=", commandDescription = "Destroy a minimesos cluster")
public class CommandDestroy implements Command {

    public static final String CLINAME = "destroy";

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
            cluster.destroy();
            ClusterRepository.deleteCluster();
        }
    }
}
