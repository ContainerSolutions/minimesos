package com.containersol.minimesos.config;

public class MesosDNSConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String MESOS_DNS_IMAGE_NAME = "xebia/mesos-dns"
    public static final String MESOS_DNS_TAG_NAME = "0.0.5"

    public MesosDNSConfig() {
        imageName = MESOS_DNS_IMAGE_NAME
        imageTag = MESOS_DNS_TAG_NAME
    }

}
