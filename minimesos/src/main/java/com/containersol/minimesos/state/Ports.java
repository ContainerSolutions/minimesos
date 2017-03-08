package com.containersol.minimesos.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Maps Mesos task ports information from JSON string to a Java object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ports {

    private List<Port> ports;

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }
}
