package com.containersol.minimesos.config;

public class ConsulConfig extends GroovyBlock implements ContainerConfig {

    public static final String CONSUL_IMAGE_NAME = "containersol/consul-server";
    public static final String CONSUL_TAG_NAME = "0.6";

    public static final int DEFAULT_CONSUL_PORT= 8500;

    String imageName     = CONSUL_IMAGE_NAME
    String imageTag      = CONSUL_TAG_NAME

}
