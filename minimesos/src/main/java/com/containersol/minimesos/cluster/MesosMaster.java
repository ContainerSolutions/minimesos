package com.containersol.minimesos.cluster;

import com.fasterxml.jackson.databind.util.Annotations;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Functionality of Mesos Master
 */
public interface MesosMaster extends MesosContainer {

    String getStateUrl();
    void waitFor();

    Map<String, String> getFlags() throws UnirestException;
}
