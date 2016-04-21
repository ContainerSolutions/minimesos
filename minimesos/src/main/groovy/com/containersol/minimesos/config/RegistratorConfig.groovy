package com.containersol.minimesos.config;

public class RegistratorConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String REGISTRATOR_IMAGE_NAME = "gliderlabs/registrator"
    public static final String REGISTRATOR_TAG_NAME = "v6"

    public RegistratorConfig() {
        imageName = REGISTRATOR_IMAGE_NAME
        imageTag = REGISTRATOR_TAG_NAME
    }

}
