package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.Consul;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;

/**
 * This is the Consul-in-a-container container. Consul adds service discovery through DNS, and a distributed k/v store.
 */
public class ConsulContainer extends AbstractContainer implements Consul {

    private final ConsulConfig config;

    public ConsulContainer(ConsulConfig config) {
        super(config);
        this.config = config;
    }

    public ConsulContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new ConsulConfig());
    }

    private ConsulContainer(MesosCluster cluster, String uuid, String containerId, ConsulConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    @Override
    public String getRole() {
        return "consul";
    }

    @Override
    protected int getServicePort() {
        return ConsulConfig.CONSUL_HTTP_PORT;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort consulHTTPPort = ExposedPort.tcp(ConsulConfig.CONSUL_HTTP_PORT);
        ExposedPort consulDNSPort = ExposedPort.udp(ConsulConfig.CONSUL_DNS_PORT);

        return DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withEnv(newEnvironment()
                        .withValue("SERVICE_IGNORE", "1")
                        .createEnvironment())
                .withExposedPorts(consulHTTPPort, consulDNSPort);
    }

}
