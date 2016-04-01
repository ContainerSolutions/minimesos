package com.containersol.minimesos.cluster;

/**
 * Functionality of Mesos Cluster core members
 */
public interface MesosContainer extends AbstractContainer {

    void setZooKeeper(ZooKeeper zookeeper);

}
