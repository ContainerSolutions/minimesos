package com.containersol.minimesos.config

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

@Slf4j
class MesosMasterConfig extends MesosContainerConfig {

    public static final String MESOS_MASTER_IMAGE = "containersol/mesos-master"
    public static final int MESOS_MASTER_PORT = 5050

    String imageName     = MESOS_MASTER_IMAGE
    String imageTag      = MESOS_IMAGE_TAG
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
