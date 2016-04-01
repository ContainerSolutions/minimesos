package com.containersol.minimesos.cluster;

/**
 * Expected from ZooKeeper functionality
 */
public interface ZooKeeper extends AbstractContainer {

    /**
     * @return ZooKeeper URL based on real IP address
     */
    String getFormattedZKAddress();

}
