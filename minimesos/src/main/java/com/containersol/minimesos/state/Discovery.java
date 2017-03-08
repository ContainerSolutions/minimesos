package com.containersol.minimesos.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps Mesos task discovery information from JSON string to a Java object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Discovery {

    private Ports ports;

    public Ports getPorts() {
        return ports;
    }

    public void setPorts(Ports ports) {
        this.ports = ports;
    }
}
