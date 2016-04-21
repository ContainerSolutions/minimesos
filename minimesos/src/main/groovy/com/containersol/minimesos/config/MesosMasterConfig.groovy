package com.containersol.minimesos.config

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

@Slf4j
class MesosMasterConfig extends MesosContainerConfig {

    public static final String MESOS_MASTER_IMAGE = "containersol/mesos-master"
    public static final int MESOS_MASTER_PORT = 5050

    public MesosMasterConfig() {
        imageName = MESOS_MASTER_IMAGE
        imageTag = MESOS_IMAGE_TAG
    }

    boolean authenticate = false
    String aclJson

}
