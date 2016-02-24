package com.containersol.minimesos.mesos;

import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * Base, unmolested Mesos master class
 */
public class Consul extends AbstractContainer {

    private final ConsulConfig config;

    public Consul(DockerClient dockerClient, ConsulConfig config) {
        super(dockerClient);
        this.config = config;
    }

    @Override
    public String getRole() {
        return "consul";
    }

    @Override
    protected void pullImage() {
        pullImage(config.getImageName(), config.getImageTag());
    }

    @Override
    public void start(int timeout) {
        super.start(timeout);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName( getName() )
                .withExposedPorts(new ExposedPort(ConsulConfig.DEFAULT_CONSUL_PORT));
    }
}
