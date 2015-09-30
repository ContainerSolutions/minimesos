package com.containersol.minimesos.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by peldan on 09/07/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {

    private String name;
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
