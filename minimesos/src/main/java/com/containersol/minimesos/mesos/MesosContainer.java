package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;

import java.util.TreeMap;

/**
 * Superclass for Mesos images
 */
public abstract class MesosContainer extends AbstractContainer {

    public static final String MESOS_IMAGE_TAG = "0.25.0-0.2.70.ubuntu1404.b1";
    public static final String DEFAULT_MESOS_ZK_PATH = "/mesos";
    public static final int DEFAULT_TIMEOUT_SEC = 60;

    private String mesosImageTag = MESOS_IMAGE_TAG;

    private final ZooKeeper zooKeeperContainer;

    protected MesosContainer(DockerClient dockerClient, ZooKeeper zooKeeperContainer) {
        super(dockerClient);
        this.zooKeeperContainer = zooKeeperContainer;
    }

    public abstract String getMesosImageName();

    protected abstract TreeMap<String, String> getDefaultEnvVars();

    @Override
    protected void pullImage() {
        pullImage(getMesosImageName(), getMesosImageTag());
    }

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public void setMesosImageTag(String mesosImageTag) {
        this.mesosImageTag = mesosImageTag;
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
        envs.put("MESOS_LOGGING_LEVEL", "INFO");
        envs.put("MESOS_WORK_DIR", "/tmp/mesos");
        return envs;
    }

    public ZooKeeper getZooKeeperContainer() {
        return zooKeeperContainer;
    }

    public String getFormattedZKAddress() {
        return getFormattedZKAddress(zooKeeperContainer);
    }

    public static String getFormattedZKAddress(ZooKeeper zkContainer) {
        return ZooKeeper.formatZKAddress(zkContainer.getIpAddress()) + DEFAULT_MESOS_ZK_PATH;
    }
}
