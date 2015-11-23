package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * Base zookeeper class
 */
public class ZooKeeper extends AbstractContainer {
    public static final String ZOOKEEPER_LOCAL_IMAGE = "jplock/zookeeper";
    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;
    public static final String ZOOKEEPER_IMAGE_TAG = "3.4.6";

    protected ZooKeeper(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    protected void pullImage() {
        pullImage(ZOOKEEPER_LOCAL_IMAGE, getZooKeeperImageTag());
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(ZOOKEEPER_LOCAL_IMAGE + ":" + getZooKeeperImageTag())
                .withName("minimesos-zookeeper-" + MesosCluster.getClusterId() + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(DEFAULT_ZOOKEEPER_PORT), new ExposedPort(2888), new ExposedPort(3888));
    }

    protected String getZooKeeperImageTag() {
        return ZOOKEEPER_IMAGE_TAG;
    }

    public static String formatZooKeeperAddress(String ipAddress) {
        return "zk://" + ipAddress + ":" + DEFAULT_ZOOKEEPER_PORT;
    }
}
