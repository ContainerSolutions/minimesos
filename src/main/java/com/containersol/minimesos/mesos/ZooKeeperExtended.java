package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

public class ZooKeeperExtended extends ZooKeeper {

    private final String clusterId;

    public ZooKeeperExtended(DockerClient dockerClient, String clusterId) {
        super(dockerClient);
        this.clusterId = clusterId;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
                .withName("minimesos-zookeeper-" + clusterId + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(2181), new ExposedPort(2888), new ExposedPort(3888));
    }

}
