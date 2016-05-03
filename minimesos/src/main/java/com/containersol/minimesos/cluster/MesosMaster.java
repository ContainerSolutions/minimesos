package com.containersol.minimesos.cluster;

/**
 * Functionality of Mesos Master
 */
public interface MesosMaster extends MesosContainer {

    String getStateUrl();

    void waitFor();

}
