package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MesosContainerConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.TreeMap;

/**
 * Superclass for Mesos images
 */
public abstract class MesosContainer extends AbstractContainer {

    public static final String DEFAULT_MESOS_ZK_PATH = "/mesos";

    private ZooKeeper zooKeeperContainer;
    private final MesosContainerConfig config;

    protected MesosContainer(DockerClient dockerClient, ZooKeeper zooKeeperContainer, MesosContainerConfig config) {
        super(dockerClient);
        this.zooKeeperContainer = zooKeeperContainer;
        this.config = config;
    }

    protected MesosContainer(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId, MesosContainerConfig config) {
        super(dockerClient, cluster, uuid, containerId);
        this.config = config;
    }

    public abstract int getPortNumber();

    protected abstract TreeMap<String, String> getDefaultEnvVars();

    @Override
    protected void pullImage() {
        pullImage(getMesosImageName(), getMesosImageTag());
    }

    public String getMesosImageTag() {
        String imageTag = config.getImageTag();
        if (MesosContainerConfig.MESOS_TAG.equalsIgnoreCase(imageTag)) {
            String mesosVersion = getCluster().getMesosVersion();
            imageTag = MesosContainerConfig.MESOS_IMAGE_TAGS.get(mesosVersion);
        }
        return imageTag;
    }

    public String getMesosImageName() {
        return config.getImageName();
    }

    protected String[] createMesosLocalEnvironment() {
        TreeMap<String, String> map = getDefaultEnvVars();
        map.putAll(getSharedEnvVars());
        return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    protected TreeMap<String, String> getSharedEnvVars() {
        TreeMap<String,String> envs = new TreeMap<>();
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
        return Unirest.get(getStateUrl()).asJson().getBody().getObject();
    }

    public String getLoggingLevel() {
        String level = config.getLoggingLevel();
        if (MesosContainerConfig.MESOS_LOGGING_LEVEL_INHERIT.equalsIgnoreCase(level)) {
            level = getCluster().getLoggingLevel();
        }
        return level;
    }

}
