package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class MesosMasterConfig extends MesosContainerConfig {

    static final String MESOS_MASTER_IMAGE = "containersol/mesos-master"
    public static int MESOS_MASTER_PORT = 5050

    String imageName     = MESOS_MASTER_IMAGE
    String imageTag      = MESOS_IMAGE_TAG

}
