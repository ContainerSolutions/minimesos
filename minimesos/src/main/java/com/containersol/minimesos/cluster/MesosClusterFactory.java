package com.containersol.minimesos.cluster;

/**
 * Interface for creating members of the cluster
 */
public abstract class MesosClusterFactory {

    public abstract ZooKeeper createZooKeeper(MesosCluster mesosCluster, String uuid, String containerId);
    public abstract MesosAgent createMesosAgent(MesosCluster mesosCluster, String uuid, String containerId);
    public abstract MesosMaster createMesosMaster(MesosCluster mesosCluster, String uuid, String containerId);
    public abstract Marathon createMarathon(MesosCluster mesosCluster, String uuid, String containerId);
    public abstract Consul createConsul(MesosCluster mesosCluster, String uuid, String containerId);
    public abstract Registrator createRegistrator(MesosCluster mesosCluster, String uuid, String containerId);

    /**
     * Fills given cluster with discovered members
     *
     * @param cluster to load with discovered members
     */
    public abstract void loadRunningCluster(MesosCluster cluster);

    /**
     * Destroys members of the cluster with given ID
     * @param clusterId ID of the cluster to destroy
     */
    public abstract void destroyRunningCluster(String clusterId);

}
