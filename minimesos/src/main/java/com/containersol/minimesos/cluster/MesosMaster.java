package com.containersol.minimesos.cluster;

import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.Map;

/**
 * Functionality of Mesos Master
 */
public interface MesosMaster extends MesosContainer {

    String getStateUrl();
    void waitFor();

    Map<String, String> getFlags() throws UnirestException;
}
