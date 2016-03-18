package com.containersol.minimesos.config

import org.apache.commons.lang.StringUtils

public class ZooKeeperConfig extends GroovyBlock implements ContainerConfig {

    public static final String MESOS_LOCAL_IMAGE = "jplock/zookeeper"
    public static final String ZOOKEEPER_IMAGE_TAG = "3.4.6"

    String imageName     = MESOS_LOCAL_IMAGE
    String imageTag      = ZOOKEEPER_IMAGE_TAG
    String networkMode   = DEFAULT_NETWORK_MODE

    @Override
    String getNetworkMode() {
        return networkMode
    }

    @Override
    void setNetworkMode(String networkMode) {
        if (!StringUtils.equalsIgnoreCase(networkMode, "bridge") && !StringUtils.equalsIgnoreCase(networkMode, "host")) {
            throw new RuntimeException("Property 'networkMode' can only have the values 'bridge' or 'host'")
        }
        this.networkMode = networkMode
    }

}
