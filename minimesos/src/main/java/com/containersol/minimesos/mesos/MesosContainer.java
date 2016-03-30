package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MesosContainerConfig;
import com.containersol.minimesos.container.AbstractContainer;
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
public abstract class MesosContainer extends AbstractContainer {

    public static final String DEFAULT_MESOS_ZK_PATH = "/mesos";

    private ZooKeeper zooKeeperContainer;
    private final MesosContainerConfig config;

    protected MesosContainer(ZooKeeper zooKeeperContainer, MesosContainerConfig config) {
        super();
        this.zooKeeperContainer = zooKeeperContainer;
        this.config = config;
    }

    protected MesosContainer(MesosCluster cluster, String uuid, String containerId, MesosContainerConfig config) {
        super(cluster, uuid, containerId);
        this.config = config;
    }

    public abstract int getPortNumber();

    protected abstract Map<String, String> getDefaultEnvVars();

    @Override
    protected void pullImage() {
        pullImage(getMesosImageName(), getMesosImageTag());
    }

    public String getMesosImageTag() {
        String imageTag = config.getImageTag();
        if (MesosContainerConfig.MESOS_IMAGE_TAG.equalsIgnoreCase(imageTag)) {
            String mesosVersion = getCluster().getMesosVersion();
            imageTag = MesosContainerConfig.MESOS_IMAGE_TAGS.get(mesosVersion);
        }
        return imageTag;
    }

    public String getMesosImageName() {
        return config.getImageName();
    }

    protected String[] createMesosLocalEnvironment() {
        envVars.putAll(getDefaultEnvVars());
        envVars.putAll(getSharedEnvVars());
        return createEnvironment();
    }

    protected Map<String, String> getSharedEnvVars() {
        Map<String, String> envs = new TreeMap<>();
        envs.put("GLOG_v", "1");
        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        envs.put("MESOS_LOGGING_LEVEL", getLoggingLevel());
        envs.put("MESOS_WORK_DIR", "/tmp/mesos");
        return envs;
    }

    public void setZooKeeperContainer(ZooKeeper zooKeeperContainer) {
        this.zooKeeperContainer = zooKeeperContainer;
    }

    public ZooKeeper getZooKeeperContainer() {
        return zooKeeperContainer;
    }

    public String getFormattedZKAddress() {
        return zooKeeperContainer.getFormattedZKAddress() + DEFAULT_MESOS_ZK_PATH;
    }

    public String getStateUrl() {
        return "http://" + getIpAddress() + ":" + getPortNumber() + "/state.json";
    }

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

}
