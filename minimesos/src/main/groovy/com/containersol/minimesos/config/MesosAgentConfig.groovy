package com.containersol.minimesos.config

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

@Slf4j
class MesosAgentConfig extends MesosContainerConfig {

    public static final String MESOS_AGENT_IMAGE        = "containersol/mesos-agent"
    public static final int DEFAULT_MESOS_AGENT_PORT    = 5051

    int portNumber          = DEFAULT_MESOS_AGENT_PORT
    String imageName        = MESOS_AGENT_IMAGE
    String imageTag         = MESOS_IMAGE_TAG

    AgentResourcesConfig resources = new AgentResourcesConfig()

    def resources(@DelegatesTo(AgentResourcesConfig) Closure cl) {
        delegateTo(resources, cl)
    }

    String networkMode   = DEFAULT_NETWORK_MODE

    @Override
    String getNetworkMode() {
        return networkMode
    }

    @Override
    void setNetworkMode(String networkMode) {
        if (!StringUtils.equalsIgnoreCase(networkMode, "bridge") && !StringUtils.equalsIgnoreCase(loggingLevel, "host")) {
            throw new RuntimeException("Property 'networkMode' can only have the values 'bridge' or 'host'")
        }
        this.networkMode = networkMode
    }

}
