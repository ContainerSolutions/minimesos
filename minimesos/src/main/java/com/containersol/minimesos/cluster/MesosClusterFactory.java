package com.containersol.minimesos.cluster;

import com.containersol.minimesos.config.ClusterConfig;
import com.github.dockerjava.api.model.Container;

import java.io.File;

/**
 * Interface for creating members of the cluster and destroying running cluster
 */
public interface MesosClusterFactory {

    /**
     * Finds MesosCluster by ID.
     *
     * @param clusterId ID of the cluster
     *
     * @return MesosCluster
     */
    MesosCluster retrieveMesosCluster(String clusterId);

    ZooKeeper createZooKeeper(MesosCluster mesosCluster, String uuid, String containerId);

    MesosAgent createMesosAgent(MesosCluster mesosCluster, String uuid, String containerId);

    MesosMaster createMesosMaster(MesosCluster mesosCluster, String uuid, String containerId);

    Marathon createMarathon(MesosCluster mesosCluster, String uuid, String containerId);

    Consul createConsul(MesosCluster mesosCluster, String uuid, String containerId);

    Registrator createRegistrator(MesosCluster mesosCluster, String uuid, String containerId);

    MesosDns createMesosDns(MesosCluster cluster, String uuid, String containerId);

    void restoreMapToPorts(MesosCluster cluster, Container container);

    /**
     * Destroys members of the cluster with given ID
     *
     * @param clusterId ID of the cluster to destroy
     */
    void destroyRunningCluster(String clusterId);

    /**
     * Loads a Mesos cluster
     */
    MesosCluster retrieveMesosCluster();

    /**
     * Returns the file that contains the cluster ID
     *
     * @return minimesos file
     */
    File getStateFile();

    /**
     * Writes the cluster ID to the minimesos file
     *
     * @param cluster cluster to store ID for
     */
    void saveStateFile(MesosCluster cluster);

    /**
     * Deletes minimesos file file
     */
    void deleteStateFile();

    /**
     * Creates a MesosCluster based on a ClusterConfig
     *
     * @param clusterConfig configuration of the Mesos cluster
     *
     * @return Mesos cluster
     */
    MesosCluster createMesosCluster(ClusterConfig clusterConfig);

}
