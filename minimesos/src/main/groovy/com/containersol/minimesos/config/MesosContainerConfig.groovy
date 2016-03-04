package com.containersol.minimesos.config

abstract class MesosContainerConfig extends GroovyBlock implements ContainerConfig {

    public static final String MESOS_IMAGE_TAG = "0.25.0-0.2.70.ubuntu1404"

    public static final String MESOS_LOGGING_LEVEL = "INFO"

    String loggingLevel = MESOS_LOGGING_LEVEL

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel.toUpperCase()
    }
}
