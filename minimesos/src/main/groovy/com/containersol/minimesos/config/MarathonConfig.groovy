package com.containersol.minimesos.config

import org.apache.commons.lang.StringUtils;

public class MarathonConfig extends GroovyBlock implements ContainerConfig {

    public static final String MARATHON_IMAGE = "mesosphere/marathon"
    public static final String MARATHON_IMAGE_TAG = "v0.15.3"
    public static final int MARATHON_PORT = 8080;

    String imageName     = MARATHON_IMAGE
    String imageTag      = MARATHON_IMAGE_TAG
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
