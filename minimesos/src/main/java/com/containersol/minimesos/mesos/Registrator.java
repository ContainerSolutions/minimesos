package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.config.RegistratorConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;

public class Registrator extends AbstractContainer {

    private final RegistratorConfig config;
    private Consul consulContainer;

    public Registrator(DockerClient dockerClient, Consul consulContainer, RegistratorConfig config) {
        super(dockerClient);
        this.consulContainer = consulContainer;
        this.config = config;
    }

    @Override
    public String getRole() {
        return "registrator";
    }

    @Override
    protected void pullImage() {
        pullImage(config.getImageName(), config.getImageTag());
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withNetworkMode("host")
                .withBinds(Bind.parse("/var/run/docker.sock:/tmp/docker.sock"))
                .withCmd("-internal", String.format("consul://%s:%d", consulContainer.getIpAddress(), ConsulConfig.CONSUL_HTTP_PORT))
                .withName(getName());
    }

    public Registrator(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        this(dockerClient, cluster, uuid, containerId, new RegistratorConfig());
    }

    private Registrator(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId, RegistratorConfig config) {
        super(dockerClient, cluster, uuid, containerId);
        this.config = config;
    }

}
