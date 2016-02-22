package com.containersol.minimesos.config

class ResourceDef extends GroovyBlock {

    String role
    def value

    public ResourceDef() {

    }

    public ResourceDef(String role, def value) {
        this.role = role
        this.value = value
    }

}
