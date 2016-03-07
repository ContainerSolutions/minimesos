package com.containersol.minimesos.config

abstract class MesosContainerConfig extends GroovyBlock implements ContainerConfig {

    public static final String MESOS_TAG = "INHERIT"

    public static final String MESOS_LOGGING_LEVEL_INHERIT = "INHERIT"

    public static final HashMap<String, String> MESOS_IMAGE_TAGS = new HashMap<>();

    static {
        MESOS_IMAGE_TAGS.put("0.25", "0.25.0-0.2.70.ubuntu1404");
        MESOS_IMAGE_TAGS.put("0.26", "0.26.0-0.2.145.ubuntu1404");
        MESOS_IMAGE_TAGS.put("0.27", "0.27.0-0.2.190.ubuntu1404");
    }

    String loggingLevel = MESOS_LOGGING_LEVEL_INHERIT

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel.toUpperCase()
    }
}
