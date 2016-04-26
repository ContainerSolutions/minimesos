package com.containersol.minimesos.config

public class ZooKeeperConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String DEFAULT_MESOS_ZK_PATH = "/mesos";
    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    public static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper"
    public static final String ZOOKEEPER_IMAGE_TAG = "3.4.6"

    public ZooKeeperConfig() {
        imageName = MESOS_LOCAL_IMAGE
        imageTag = ZOOKEEPER_IMAGE_TAG
    }

}
