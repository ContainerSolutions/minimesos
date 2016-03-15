package com.containersol.minimesos.config

abstract class MesosContainerConfig extends GroovyBlock implements ContainerConfig {

    public static final String MESOS_IMAGE_TAG = "# derive from mesos version"
    public static final String MESOS_LOGGING_LEVEL_INHERIT = "# INHERIT FROM CLUSTER"

    public static final HashMap<String, String> MESOS_IMAGE_TAGS = [
            "0.25": "0.25.0-0.2.70.ubuntu1404",
            "0.26": "0.26.0-0.2.145.ubuntu1404",
            "0.27": "0.27.1-2.0.226.ubuntu1404"
    ]

    private String loggingLevel = MESOS_LOGGING_LEVEL_INHERIT

    public String getLoggingLevel() {
        return loggingLevel
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel.toUpperCase()
    }
}
