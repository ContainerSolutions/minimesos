package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services.
 */
public class ZooKeeperContainer extends AbstractContainer implements ZooKeeper {

    private final ZooKeeperConfig config;

    public ZooKeeperContainer(ZooKeeperConfig config) {
        super();
        this.config = config;
    }

    protected ZooKeeperContainer() {
        this(new ZooKeeperConfig());
    }

    public ZooKeeperContainer(MesosCluster cluster, String uuid, String containerId) {
        super(cluster, uuid, containerId);
        this.config = new ZooKeeperConfig();
    }

    @Override
    public String getRole() {
        return "zookeeper";
    }

    @Override
    protected void pullImage() {
        pullImage(config.getImageName(), config.getImageTag());
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.getDockerClient().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withExposedPorts(new ExposedPort(ZooKeeperConfig.DEFAULT_ZOOKEEPER_PORT), new ExposedPort(2888), new ExposedPort(3888));
    }

    /**
     * @return ZooKeeper URL based on real IP address
     */
    @Override
    public String getFormattedZKAddress() {
        return "zk://" + getIpAddress() + ":" + ZooKeeperConfig.DEFAULT_ZOOKEEPER_PORT;
    }

}
