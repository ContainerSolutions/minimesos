package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.log4j.Logger;
import com.containersol.minimesos.container.AbstractContainer;

import java.security.SecureRandom;

public class ZooKeeper extends AbstractContainer {
    private static Logger LOGGER = Logger.getLogger(ZooKeeper.class);
    private static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper";
    public static final String REGISTRY_TAG = "3.4.5";


    public ZooKeeper(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    public void start() {
        super.start();
    }


    String generateContainerName() {
        return "minimesos-zookeeper-" + Integer.toUnsignedString(new SecureRandom().nextInt());
    }


    @Override
    protected void pullImage() {
        pullImage(MESOS_LOCAL_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
                .withName(this.generateContainerName())
                .withExposedPorts(new ExposedPort(2181), new ExposedPort(2888), new ExposedPort(3888));
    }
}
