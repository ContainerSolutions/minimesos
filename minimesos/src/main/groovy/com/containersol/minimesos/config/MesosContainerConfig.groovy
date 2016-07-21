package com.containersol.minimesos.config

abstract class MesosContainerConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String MESOS_LOGGING_LEVEL_INHERIT = "# INHERIT FROM CLUSTER"
    public static final String MINIMESOS_DOCKER_TAG = "0.1.0"

    private String loggingLevel = MESOS_LOGGING_LEVEL_INHERIT

    public static final List<String> MESOS_VERSIONS = [
            "0.25",
            "0.25.0",
            "0.26",
            "0.27",
            "0.28.0",
            "0.28.1",
            "0.28",
    ]

    public String getLoggingLevel() {
        return loggingLevel
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel.toUpperCase()
    }
}
