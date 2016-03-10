package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

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
    protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(ConsulConfig.DEFAULT_CONSUL_PORT);
        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(exposedPort, Ports.Binding(ConsulConfig.DEFAULT_CONSUL_PORT));
        }
        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName( getName() )
                .withPortBindings(portBindings)
                .withExposedPorts(exposedPort);
    }

    public Consul(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        this(dockerClient, cluster, uuid, containerId, new ConsulConfig());
    }

    private Consul(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId, ConsulConfig config) {
        super(dockerClient, cluster, uuid, containerId);
        this.config = config;
    }

}
