package com.containersol.minimesos.config

public class ZooKeeperConfig extends GroovyBlock implements ContainerConfig {

    public static final int DEFAULT_ZOOKEEPER_PORT = 2181;

    public static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper"
    public static final String ZOOKEEPER_IMAGE_TAG = "3.4.6"

    String imageName     = MESOS_LOCAL_IMAGE
    String imageTag      = ZOOKEEPER_IMAGE_TAG

}
