package com.containersol.minimesos.config;

import org.apache.commons.lang.StringUtils;

public class RegistratorConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String REGISTRATOR_IMAGE_NAME = "gliderlabs/registrator"
    public static final String REGISTRATOR_TAG_NAME = "v6"

    public RegistratorConfig() {
        imageName     = REGISTRATOR_IMAGE_NAME
        imageTag      = REGISTRATOR_TAG_NAME
    }

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
