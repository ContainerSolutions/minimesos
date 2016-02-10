package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * Base zookeeper class
 */
public class ZooKeeper extends AbstractContainer {

    public static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper";
    public static final String ZOOKEEPER_IMAGE_TAG = "3.4.6";
    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    private String zooKeeperImageTag = ZOOKEEPER_IMAGE_TAG;

    protected ZooKeeper(DockerClient dockerClient, String zooKeeperImageTag) {
        super(dockerClient);
        this.zooKeeperImageTag = zooKeeperImageTag;
    }

    protected ZooKeeper(DockerClient dockerClient) {
        this(dockerClient, ZOOKEEPER_IMAGE_TAG);
    }

    public ZooKeeper(DockerClient dockerClient, String clusterId, String uuid, String containerId) {
        super(dockerClient, clusterId, uuid, containerId);
    }

    @Override
    protected String getRole() {
        return "zookeeper";
    }

    @Override
    protected void pullImage() {
        pullImage(MESOS_LOCAL_IMAGE, zooKeeperImageTag);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + zooKeeperImageTag)
                .withName( getName() )
                .withExposedPorts(new ExposedPort(DEFAULT_ZOOKEEPER_PORT), new ExposedPort(2888), new ExposedPort(3888));
    }

    public static String formatZKAddress(String ipAddress) {
        return "zk://" + ipAddress + ":" + DEFAULT_ZOOKEEPER_PORT;
    }
}
