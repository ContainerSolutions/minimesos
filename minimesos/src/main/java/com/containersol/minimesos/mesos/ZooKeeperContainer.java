package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosDns;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.integrationtest.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.util.Environment;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services.
 */
public class ZooKeeperContainer extends AbstractContainer implements ZooKeeper {

    private final ZooKeeperConfig config;

    public ZooKeeperContainer(ZooKeeperConfig config) {
        super(config);
        this.config = config;
    }

    protected ZooKeeperContainer() {
        this(new ZooKeeperConfig());
    }

    public ZooKeeperContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new ZooKeeperConfig());
    }

    public ZooKeeperContainer(MesosCluster cluster, String uuid, String containerId, ZooKeeperConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    @Override
    public String getRole() {
        return "zookeeper";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
            .withName(getName())
            .withExposedPorts(new ExposedPort(ZooKeeperConfig.DEFAULT_ZOOKEEPER_PORT), new ExposedPort(2888), new ExposedPort(3888));
    }

    @Override
    protected String getServiceProtocol() {
        return "zk";
    }

    @Override
    protected int getServicePort() {
        return ZooKeeperConfig.DEFAULT_ZOOKEEPER_PORT;
    }

    @Override
    protected String getServicePath() {
        return ZooKeeperConfig.DEFAULT_MESOS_ZK_PATH;
    }

    /**
     * @return ZooKeeper URL based on real IP address
     */
    @Override
    public String getFormattedZKAddress() {
        return "zk://" + getIpAddress() + ":" + ZooKeeperConfig.DEFAULT_ZOOKEEPER_PORT;
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

}
