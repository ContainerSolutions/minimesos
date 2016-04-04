package com.containersol.minimesos.cluster;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

/**
 * Functionality of Mesos Cluster core members
 */
public interface MesosContainer extends ClusterMember {

    void setZooKeeper(ZooKeeper zookeeper);
    JSONObject getStateInfoJSON() throws UnirestException;

    String getMesosImageTag();
    String getLoggingLevel();

}
