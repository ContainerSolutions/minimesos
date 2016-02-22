package com.containersol.minimesos.config

/**
 * Common methods for containers' configuration
 */
interface ContainerConfig {

    String getImageName()
    String getImageTag()

}