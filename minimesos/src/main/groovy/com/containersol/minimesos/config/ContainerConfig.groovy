package com.containersol.minimesos.config

/**
 * Common methods for containers' configuration
 */
interface ContainerConfig {

    String getImageName()

    void setImageName(String imageName)

    String getImageTag()

    void setImageTag(String imageTag)

}
