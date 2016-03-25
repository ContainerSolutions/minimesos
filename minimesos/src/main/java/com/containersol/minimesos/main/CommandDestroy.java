package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameters for the 'destroy' command.
 */
@Parameters(separators = "=", commandDescription = "Destroy a minimesos cluster")
public class CommandDestroy implements Command {

    private static Logger LOGGER = LoggerFactory.getLogger(CommandDestroy.class);

    public static final String CLINAME = "destroy";

    @Override
    public void execute() {
        MesosCluster cluster = ClusterRepository.loadCluster();
        if (cluster != null) {
            cluster.destroy();
            ClusterRepository.deleteClusterFile();
        } else {
            LOGGER.info("Minimesos cluster is not running");
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
