package com.containersol.minimesos.cluster;

import com.containersol.minimesos.state.State;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

/**
 * Functionality of Mesos Cluster core members
 */
public interface MesosContainer extends ClusterProcess {

    void setZooKeeper(ZooKeeper zookeeper);

    JSONObject getStateInfoJSON() throws UnirestException;

    /**
     * Retrieve state of the Master or Agent.
     *
     * @return state object with frameworks and tasks
     */
    State getState();

}
