package com.containersol.minimesos.cluster;

import java.net.URI;

/**
 * Generic functionality of every cluster member
 */
public interface ClusterProcess {

    MesosCluster getCluster();

    void setCluster(MesosCluster mesosCluster);

    /**
     * @return the IP address of the container
     */
    String getIpAddress();

    /**
     *
     */
    URI getServiceUrl();

    /**
     * Builds container name following the naming convention
     *
     * @return container name
     */
    String getName();

    /**
     * @return the ID of the container.
     */
    String getContainerId();

    /**
     * Starts the container and waits until is started
     *
     * @param timeout in seconds
     */
    void start(int timeout);

    String getRole();

    /**
     * Removes a container with force
     */
    void remove();

}
