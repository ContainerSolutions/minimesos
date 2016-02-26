package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class ConfigParser {

    ClusterConfig parse(String config) {
        Binding binding = new Binding();

        ClusterConfig minimesosDsl = new ClusterConfig()
        binding.setVariable("minimesos", minimesosDsl)

        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(config)
        script.run()

        return minimesosDsl
    }

}