package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class MesosAgentConfig extends MesosContainerConfig {

    public static final String MESOS_SLAVE_IMAGE = "containersol/mesos-agent"
    public static final int DEFAULT_MESOS_SLAVE_PORT = 5051

    int portNumber = DEFAULT_MESOS_SLAVE_PORT

    String imageName        = MESOS_SLAVE_IMAGE
    String imageTag         = MESOS_IMAGE_TAG

    AgentResourcesConfig resources = new AgentResourcesConfig()

    def resources(@DelegatesTo(AgentResourcesConfig) Closure cl) {
        delegateTo(resources, cl)
    }

}
