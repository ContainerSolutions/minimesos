package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.Consul;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.Registrator;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.config.RegistratorConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;

/**
 * Registrator automatically registers and deregisters services for any Docker container by inspecting containers as they come online.
 */
public class RegistratorContainer extends AbstractContainer implements Registrator {

    private final RegistratorConfig config;
    private Consul consulContainer;

    public RegistratorContainer(Consul consulContainer, RegistratorConfig config) {
        super(config);
        this.consulContainer = consulContainer;
        this.config = config;
    }

    public RegistratorContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new RegistratorConfig());
    }

    private RegistratorContainer(MesosCluster cluster, String uuid, String containerId, RegistratorConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    @Override
    public String getRole() {
        return "registrator";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.getDockerClient().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withNetworkMode("host")
                .withBinds(Bind.parse("/var/run/docker.sock:/tmp/docker.sock"))
                .withCmd("-internal", String.format("consul://%s:%d", consulContainer.getIpAddress(), ConsulConfig.CONSUL_HTTP_PORT))
                .withName(getName());
    }

}
