package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

/**
 * ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services.
 */
public class ZooKeeper extends AbstractContainer {

    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    private final ZooKeeperConfig config;

    public ZooKeeper(DockerClient dockerClient, ZooKeeperConfig config) {
        super(dockerClient);
        this.config = config;
    }

    protected ZooKeeper(DockerClient dockerClient) {
        this(dockerClient, new ZooKeeperConfig());
    }

    public ZooKeeper(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        super(dockerClient, cluster, uuid, containerId);
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
        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withNetworkMode(config.getNetworkMode())
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
