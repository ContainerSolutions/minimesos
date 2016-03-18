package com.containersol.minimesos.config

import org.apache.commons.lang.StringUtils;

public class ConsulConfig extends GroovyBlock implements ContainerConfig {

    public static final String CONSUL_IMAGE_NAME = "containersol/consul-server"
    public static final String CONSUL_TAG_NAME = "0.6"

    public static final int CONSUL_HTTP_PORT = 8500
    public static final int CONSUL_DNS_PORT = 8600

    String imageName     = CONSUL_IMAGE_NAME
    String imageTag      = CONSUL_TAG_NAME
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

