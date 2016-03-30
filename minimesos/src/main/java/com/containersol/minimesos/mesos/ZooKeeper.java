package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services.
 */
public class ZooKeeper extends AbstractContainer {

    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    private final ZooKeeperConfig config;

    public ZooKeeper(ZooKeeperConfig config) {
        super();
        this.config = config;
    }

    protected ZooKeeper() {
        this(new ZooKeeperConfig());
    }

    public ZooKeeper(MesosCluster cluster, String uuid, String containerId) {
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
        return DockerClientFactory.get().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withExposedPorts(new ExposedPort(DEFAULT_ZOOKEEPER_PORT), new ExposedPort(2888), new ExposedPort(3888));
    }

    /**
     * @return ZooKeeper URL based on real IP address
     */
    public String getFormattedZKAddress() {
        return getFormattedZKAddress(getIpAddress());
    }

    /**
     * @param ipAddress overwrites real IP of ZooKeeper container
     * @return ZooKeeper URL based on given IP address
     */
    public static String getFormattedZKAddress(String ipAddress) {
        return "zk://" + ipAddress + ":" + DEFAULT_ZOOKEEPER_PORT;
    }

}
