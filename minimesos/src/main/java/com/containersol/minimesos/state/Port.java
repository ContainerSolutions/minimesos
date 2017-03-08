package com.containersol.minimesos.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps Mesos port information from JSON string to a Java object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Port {

    private int number;

    private String protocol;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
