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

    private RegistratorConfig config;

    private Consul consul;

    public RegistratorContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new RegistratorConfig());
    }

    private RegistratorContainer(MesosCluster cluster, String uuid, String containerId, RegistratorConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    public RegistratorContainer(RegistratorConfig registrator) {
        super(registrator);
        this.config = registrator;
    }

    @Override
    public String getRole() {
        return "registrator";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
         return DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withNetworkMode("host")
                .withBinds(Bind.parse("/var/run/docker.sock:/tmp/docker.sock"))
                .withCmd("-internal", String.format("consul://%s:%d", consul.getIpAddress(), ConsulConfig.CONSUL_HTTP_PORT))
                .withName(getName());
    }

    public void setConsul(ConsulContainer consul) {
        this.consul = consul;
    }

    @Override
    public void setConsul(Consul consul) {
        this.consul = consul;
    }
}
