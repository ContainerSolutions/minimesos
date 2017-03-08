package com.containersol.minimesos.config;

/**
 * Configuration for a Marathon group. Path is relative to the minimesosFile.
 */
class GroupConfig {

    private String marathonJson

    void setMarathonJson(String marathonJson) {
        this.marathonJson = marathonJson
    }

    String getMarathonJson() {
        return marathonJson
    }

}
