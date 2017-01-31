package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameters for the 'destroy' command.
 */
@Parameters(separators = "=", commandDescription = "Destroy a minimesos cluster")
public class CommandDestroy implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDestroy.class);

    public static final String CLINAME = "destroy";

    private ClusterRepository repository = new ClusterRepository();

    @Override
    public void execute() {

        MesosClusterContainersFactory clusterFactory = new MesosClusterContainersFactory();

        MesosCluster cluster = repository.loadCluster(clusterFactory);
        if (cluster != null) {
            cluster.destroy(clusterFactory);
            LOGGER.info("Destroyed minimesos cluster with ID " + cluster.getClusterId());
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
