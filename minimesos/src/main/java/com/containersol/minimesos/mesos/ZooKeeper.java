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
    public static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper";
    public static final String REGISTRY_TAG = "3.4.6";
    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    protected ZooKeeper(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    protected void pullImage() {
        pullImage(MESOS_LOCAL_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
                .withName("minimesos-zookeeper-" + MesosCluster.getClusterId() + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(DEFAULT_ZOOKEEPER_PORT), new ExposedPort(2888), new ExposedPort(3888));
    }

    public static String formatZKAddress(String ipAddress) {
        return "zk://" + ipAddress + ":" + DEFAULT_ZOOKEEPER_PORT;
    }
}
