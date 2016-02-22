package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class AgentConfig extends GroovyBlock {

    def imageName     = "containersol/mesos-agent"
    def imageTag      = "0.25"
    def loggingLevel  = "INFO"

    AgentResources resources = new AgentResources()

    def resources(@DelegatesTo(AgentResources) Closure cl) {
        delegateTo(resources, cl)
    }

}
