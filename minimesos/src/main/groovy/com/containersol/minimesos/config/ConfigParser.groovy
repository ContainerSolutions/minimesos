package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class ConfigParser {

    Cluster parse(String config) {
        Binding binding = new Binding();

        Cluster minimesosDsl = new Cluster()
        binding.setVariable("minimesos", minimesosDsl)

        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(config)
        script.run()

        return minimesosDsl
    }

}