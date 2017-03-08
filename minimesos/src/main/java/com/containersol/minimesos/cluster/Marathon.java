package com.containersol.minimesos.cluster;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import mesosphere.marathon.client.model.v2.Result;

/**
 * Functionality, which is expected from Marathon
 */
public interface Marathon extends ClusterProcess {

    /**
     * If Marathon configuration requires, installs the applications
     */
    void installMarathonApps();

    /**
     * Deploys a Marathon app by JSON string
     *
     * @param marathonJson JSON string
     */
    void deployApp(String marathonJson);

    /**
     * Updates a Marathon app by JSON string
     *
     * @param marathonJson JSON string
     */
    void updateApp(String marathonJson);

    /**
     * Kill all apps that are currently running.
     */
    void killAllApps();

    void setZooKeeper(ZooKeeper zookeeper);

    /**
     * Delete the given app
     *
     * @param app to be deleted
     */
    Result deleteApp(String app);

    /**
     * Deploy a Marathon application group.
     *
     * @param groupJson JSON string with Marathon application group definition
     */
    void deployGroup(String groupJson);

    /**
     * Deploy a Marathon application group.
     *
     * @param group group name
     */
    Result deleteGroup(String group);
}
