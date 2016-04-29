package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.Consul;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;

/**
 * This is the Consul-in-a-container container. Consul adds service discovery through DNS, and a distributed k/v store.
 */
public class ConsulContainer extends AbstractContainer implements Consul {

    public static final int DNS_PORT = 53;
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

        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(consulHTTPPort, new Ports.Binding(ConsulConfig.CONSUL_HTTP_PORT));
        }

        String gatewayIpAddress = DockerContainersUtil.getGatewayIpAddress(getCluster().getMaster().getContainerId());
        portBindings.bind(consulDNSPort, new Ports.Binding(gatewayIpAddress, DNS_PORT));

        return DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withPortBindings(portBindings)
                .withEnv(newEnvironment()
                        .withValue("SERVICE_IGNORE", "1")
                        .createEnvironment())
                .withExposedPorts(consulHTTPPort, consulDNSPort);
    }

}
