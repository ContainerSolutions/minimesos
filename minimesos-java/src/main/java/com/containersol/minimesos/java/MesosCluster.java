package com.containersol.minimesos.java;

import com.containersol.minimesos.mesos.ClusterArchitecture;
import org.apache.log4j.Logger;

/**
 * minimesos Java client
 */
public class MesosCluster extends com.containersol.minimesos.cluster.MesosCluster {

    public static final Logger LOGGER = Logger.getLogger(MesosCluster.class);

    /**
     * Create a new MesosCluster with a specified cluster architecture.
     *
     * @param clusterArchitecture Represents the layout of the cluster. See {@link ClusterArchitecture}
     */
    public MesosCluster(ClusterArchitecture clusterArchitecture) {
        super(clusterArchitecture);
    }

    public void start() {
        LOGGER.debug("Launching container containersol/minimesos-restapi");
        LOGGER.debug("Waiting until REST API is up...");
        LOGGER.debug("REST API is up");
        LOGGER.debug("POSTING cluster config");
        LOGGER.debug("Waiting until cluster is up...");
        LOGGER.debug("Cluster is up");
    }

    public void destroy() {
        LOGGER.debug("Destroying cluster");
        LOGGER.debug("Cluster is destroyed");
        LOGGER.debug("Stopping container containersol/minimesos-restapi");
        LOGGER.debug("REST API is stopped");
    }

}
