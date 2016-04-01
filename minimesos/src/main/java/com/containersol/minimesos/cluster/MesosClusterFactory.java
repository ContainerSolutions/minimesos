package com.containersol.minimesos.cluster;

import com.containersol.minimesos.mesos.Consul;
import com.containersol.minimesos.mesos.MesosAgent;
import com.containersol.minimesos.mesos.MesosMaster;
import com.containersol.minimesos.mesos.Registrator;

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

}
