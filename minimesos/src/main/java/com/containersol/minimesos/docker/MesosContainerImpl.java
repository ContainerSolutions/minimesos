package com.containersol.minimesos.docker;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosContainer;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.MesosContainerConfig;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.state.State;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Superclass for Mesos master and agent images.
 * Apache Mesos abstracts CPU, memory, storage, and other compute resources away from machines (physical or virtual), enabling fault-tolerant and elastic distributed systems to easily be built and run effectively.
 */
public abstract class MesosContainerImpl extends AbstractContainer implements MesosContainer {

    private ZooKeeper zooKeeperContainer;
    protected MesosContainerConfig config;

    protected MesosContainerImpl(MesosContainerConfig config) {
        super(config);
        this.config = config;
    }

    protected MesosContainerImpl(MesosCluster cluster, String uuid, String containerId, MesosContainerConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    @Override
    public String getImageTag() {
        String imageTag = config.getImageTag();
        if (MesosContainerConfig.MINIMESOS_DOCKER_TAG.equalsIgnoreCase(imageTag)) {
            imageTag = MesosContainerConfig.MINIMESOS_DOCKER_TAG;
        }
        return imageTag;
    }

    protected final Map<String, String> getSharedEnvVars() {
        Map<String, String> envs = new TreeMap<>();
        envs.put("GLOG_v", "1");
        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        envs.put("MESOS_LOGGING_LEVEL", getLoggingLevel());
        return envs;
    }

    @Override
    public void setZooKeeper(ZooKeeper zooKeeperContainer) {
        this.zooKeeperContainer = zooKeeperContainer;
    }

    public ZooKeeper getZooKeeper() {
        return zooKeeperContainer;
    }

    public String getFormattedZKAddress() {
        return zooKeeperContainer.getFormattedZKAddress() + ZooKeeperConfig.DEFAULT_MESOS_ZK_PATH;
    }

    public String getStateUrl() {
        return getServiceUrl().toString() + "/state.json";
    }

    @Override
    public JSONObject getStateInfoJSON() throws UnirestException {
        String stateUrl = getStateUrl();
        GetRequest request = Unirest.get(stateUrl);
        HttpResponse<JsonNode> response = request.asJson();
        return response.getBody().getObject();
    }

    public String getLoggingLevel() {
        String level = config.getLoggingLevel();
        if (MesosContainerConfig.MESOS_LOGGING_LEVEL_INHERIT.equalsIgnoreCase(level)) {
            level = getCluster().getLoggingLevel();
        }
        return level;
    }

    @Override
    public State getState() {
        try {
            return State.fromJSON(getStateInfoJSON().toString());
        } catch (JsonParseException | JsonMappingException | UnirestException e) {
            throw new MinimesosException("Could not retrieve state from Mesos container: " + getName(), e);
        }
    }
}
