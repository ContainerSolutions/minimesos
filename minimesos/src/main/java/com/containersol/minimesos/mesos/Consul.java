package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

import java.util.TreeMap;

/**
 * Base, unmolested Mesos master class
 */
public class Consul extends AbstractContainer {

    public static final String CONSUL_IMAGE_NAME = "containersol/consul-server";
    public static final String CONSUL_TAG_NAME = "0.6";

    public static final int DEFAULT_CONSUL_PORT= 8500;

    public Consul(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    protected void pullImage() {
        pullImage(CONSUL_IMAGE_NAME, CONSUL_TAG_NAME);
    }

    @Override
    public void start(int timeout) {
        super.start(timeout);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(CONSUL_IMAGE_NAME + ":" + CONSUL_TAG_NAME)
                .withName("minimesos-consul-" + getClusterId() + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(DEFAULT_CONSUL_PORT));
    }
}
