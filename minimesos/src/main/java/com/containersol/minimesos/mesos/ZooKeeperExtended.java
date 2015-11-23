package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;

public class ZooKeeperExtended extends ZooKeeper {
    private final String zookeeperImageTag;

    public ZooKeeperExtended(DockerClient dockerClient, String zookeeperImageTag) {
        super(dockerClient);
        this.zookeeperImageTag = zookeeperImageTag;
    }

    @Override
    protected void pullImage() {
        pullImage(ZooKeeper.ZOOKEEPER_LOCAL_IMAGE, getZooKeeperImageTag());
    }

    @Override
    protected String getZooKeeperImageTag() {
        return zookeeperImageTag;
    }
}