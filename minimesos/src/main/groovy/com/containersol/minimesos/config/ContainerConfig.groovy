package com.containersol.minimesos.config


/**
 * Common methods for containers' configuration
 */
interface ContainerConfig {

    public static final String DEFAULT_NETWORK_MODE = "bridge"

    String getImageName()

    void setImageName(String imageName)

    String getImageTag()

    void setImageTag(String imageTag)

    String getNetworkMode()

    void setNetworkMode(String networkMode)

}