package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class MasterConfig extends GroovyBlock {

    def imageName     = "containersol/mesos-master"
    def imageTag      = "0.25"
    def loggingLevel  = "INFO"

}
