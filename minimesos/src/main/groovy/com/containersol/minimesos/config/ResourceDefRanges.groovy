package com.containersol.minimesos.config

/**
 * Check http://mesos.apache.org/documentation/latest/attributes-resources/ for possible types of values
 */
class ResourceDefRanges extends ResourceDef {

    String value

    public ResourceDefRanges() {
    }

    public ResourceDefRanges(String role, String value) {
        super(role)
        this.value = value
    }

    @Override
    String valueAsString() {
        return value
    }
}
