package com.containersol.minimesos.config

/**
 * Configuration for a Marathon app. Path is relative to the minimesosFile.
 */
class AppConfig {

    private String marathonJson

    public void setMarathonJson(String marathonJson) {
        this.marathonJson = marathonJson
    }

    public String getMarathonJson() {
        return marathonJson
    }

}
