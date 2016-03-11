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
        ExposedPort consulHTTPPort = ExposedPort.tcp(ConsulConfig.CONSUL_HTTP_PORT);
        ExposedPort consulDNSPort = ExposedPort.udp(ConsulConfig.CONSUL_DNS_PORT);

        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(consulHTTPPort, Ports.Binding(ConsulConfig.CONSUL_HTTP_PORT));
        }
        // TODO find out docker0's IP instead of hard coding
        portBindings.bind(consulDNSPort, Ports.Binding("172.17.0.1", 53));

        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withPortBindings(portBindings)
                .withExposedPorts(consulHTTPPort, consulDNSPort);
    }

    public Consul(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        this(dockerClient, cluster, uuid, containerId, new ConsulConfig());
    }

    private Consul(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId, ConsulConfig config) {
        super(dockerClient, cluster, uuid, containerId);
        this.config = config;
    }

}
