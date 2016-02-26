package com.containersol.minimesos.config

/**
 * Check http://mesos.apache.org/documentation/latest/attributes-resources/ for possible types of values
 */
abstract class ResourceDef extends GroovyBlock {

    String role

    protected ResourceDef() {
    }

    protected ResourceDef(String role) {
        this.role = role
    }

    abstract public String valueAsString()

}
