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

/**
 * This is the Consul-in-a-container container. Consul adds service discovery through DNS, and a distributed k/v store.
 */
public class ConsulContainer extends AbstractContainer implements Consul {

    public static final int DNS_PORT = 8600;

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
    protected CreateContainerCmd dockerCommand() {
        envVars.put("SERVICE_IGNORE", "1");

        CreateContainerCmd cmd = DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withNetworkMode(getCluster().getClusterConfig().getNetworkMode())
                .withEnv(createEnvironment());

        if (getCluster().getClusterConfig().getNetworkMode().equals("bridge")) {
            cmd = bindPortsToHost(cmd);
        } else {
            cmd = cmd.withCmd("-advertise", getIpAddress());
        }

        return cmd;
    }

    private CreateContainerCmd bindPortsToHost(CreateContainerCmd cmd) {
        ExposedPort consulHTTPPort = ExposedPort.tcp(ConsulConfig.CONSUL_HTTP_PORT);
        ExposedPort consulDNSPort = ExposedPort.udp(ConsulConfig.CONSUL_DNS_PORT);

        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(consulHTTPPort, Ports.Binding(ConsulConfig.CONSUL_HTTP_PORT));
        }

        String gatewayIpAddress = DockerContainersUtil.getGatewayIpAddress();
        portBindings.bind(consulDNSPort, Ports.Binding(gatewayIpAddress, DNS_PORT));

        cmd = cmd.withPortBindings(portBindings).withExposedPorts(consulHTTPPort, consulDNSPort);
        return cmd;
    }

}
