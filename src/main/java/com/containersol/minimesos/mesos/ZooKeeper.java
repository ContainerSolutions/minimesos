package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.containersol.minimesos.container.AbstractContainer;

public class ZooKeeper extends AbstractContainer {

    private static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper";
    public static final String REGISTRY_TAG = "latest";

    private final String clusterId;

    public ZooKeeper(DockerClient dockerClient, String clusterId) {
        super(dockerClient);
        this.clusterId = clusterId;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected void pullImage() {
        pullImage(MESOS_LOCAL_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
                .withName("minimesos-zookeeper-" + clusterId + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(2181), new ExposedPort(2888), new ExposedPort(3888));
    }

}
