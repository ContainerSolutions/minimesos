package com.containersol.minimesos.config

import org.apache.commons.lang.StringUtils;

public class ContainerConfigBlock extends GroovyBlock implements ContainerConfig {

    public static final String DEFAULT_NETWORK_MODE = "bridge"

    String imageName
    String imageTag
    String networkMode = DEFAULT_NETWORK_MODE

    public ContainerConfigBlock() {

    }

    public ContainerConfigBlock(String name, String tag) {
        this.imageName = name
        this.imageTag = tag;
    }

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
