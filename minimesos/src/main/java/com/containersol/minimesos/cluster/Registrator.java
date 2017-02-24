package com.containersol.minimesos.cluster;

/**
 * Consul functionality
 */
public interface Registrator extends ClusterProcess {

    void setConsul(Consul consul);
}
