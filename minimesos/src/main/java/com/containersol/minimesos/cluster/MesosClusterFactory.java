package com.containersol.minimesos.cluster;

/**
 * Interface for creating members of the cluster and destroying running cluster
 */
public abstract class MesosClusterFactory {

    /**
     * Fills given cluster with discovered members
     *
     * @param cluster to load with discovered members
     */
    public abstract void loadRunningCluster(MesosCluster cluster);

    /**
     * Destroys members of the cluster with given ID
     *
     * @param clusterId ID of the cluster to destroy
     */
    public abstract void destroyRunningCluster(String clusterId);

}
