package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.Consul;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.integrationtest.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.util.Environment;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

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
    public URI getServiceUrl() {
        URI serviceUri = null;

        String protocol = getServiceProtocol();

        String host;
        if (Environment.isRunningInJvmOnMacOsX()) {
            host = "localhost";
        } else {
            host = getIpAddress();
        }

        int port = getServicePort();
        String path = getServicePath();

        if (StringUtils.isNotEmpty(host)) {
            try {
                serviceUri = new URI(protocol, null, host, port, path, null, null);
            } catch (URISyntaxException e) {
                throw new MinimesosException("Failed to form service URL for " + getName(), e);
            }
        }

        return serviceUri;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        int port = getServicePort();
        ExposedPort exposedPort = ExposedPort.tcp(port);

        Ports portBindings = new Ports();
        if (getCluster().isMapPortsToHost()) {
            portBindings.bind(exposedPort, Ports.Binding.bindPort(port));
        }

        ExposedPort consulHTTPPort = ExposedPort.tcp(ConsulConfig.CONSUL_HTTP_PORT);
        ExposedPort consulDNSPort = ExposedPort.udp(ConsulConfig.CONSUL_DNS_PORT);

        return DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withCmd("agent", "-server", "-bootstrap", "-client", "0.0.0.0")
                .withExposedPorts(consulHTTPPort, consulDNSPort)
                .withPortBindings(portBindings);
    }

}
