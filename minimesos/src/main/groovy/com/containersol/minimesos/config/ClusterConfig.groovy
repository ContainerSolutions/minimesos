package com.containersol.minimesos.config

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

@Slf4j
class ClusterConfig extends GroovyBlock {

    public static final int DEFAULT_TIMEOUT_SECS = 60
    public static final String DEFAULT_MESOS_VERSION = "0.25"
    public static final String DEFAULT_CONFIG_FILE = "minimesosFile"
    public static final String DEFAULT_LOGGING_LEVEL = "INFO"

    def call(Closure cl) {
        cl.setDelegate(this);
        cl.setResolveStrategy(Closure.DELEGATE_ONLY)
        cl.call();
    }

    boolean exposePorts = false
    int timeout = DEFAULT_TIMEOUT_SECS
    String mesosVersion = DEFAULT_MESOS_VERSION
    String clusterName = null
    String loggingLevel = DEFAULT_LOGGING_LEVEL

    MesosMasterConfig master = null
    List<MesosAgentConfig> agents = new ArrayList<>()
    ZooKeeperConfig zookeeper = null
    MarathonConfig marathon = null
    ConsulConfig consul = null
    RegistratorConfig registrator = null

    def master(@DelegatesTo(MesosMasterConfig) Closure cl) {
        if (master != null) {
            throw new RuntimeException("Multiple Masters are not supported in this version yet")
        }
        master = new MesosMasterConfig()
        delegateTo(master, cl)
    }

    def agent(@DelegatesTo(MesosAgentConfig) Closure cl) {
        def agent = new MesosAgentConfig()
        delegateTo(agent, cl)
        agents.add(agent)
    }

    def zookeeper(@DelegatesTo(ZooKeeperConfig) Closure cl) {
        if (zookeeper != null) {
            throw new RuntimeException("Multiple Zookeepers are not supported in this version yet")
        }
        zookeeper = new ZooKeeperConfig();
        delegateTo(zookeeper, cl)
    }

    def marathon(@DelegatesTo(MarathonConfig) Closure cl) {
        if (marathon != null) {
            throw new RuntimeException("Cannot have more than 1 marathon")
        }
        marathon = new MarathonConfig();
        delegateTo(marathon, cl)
    }

    void setLoggingLevel(String loggingLevel) {
        if (!StringUtils.equalsIgnoreCase(loggingLevel, "WARNING") && !StringUtils.equalsIgnoreCase(loggingLevel, "INFO") && !StringUtils.equalsIgnoreCase(loggingLevel, "ERROR")) {
            throw new RuntimeException("Property 'loggingLevel' can only have the values INFO, WARNING or ERROR")
        }
        this.loggingLevel = loggingLevel.toUpperCase()
    }

    void setMesosVersion(String mesosVersion) {
        if (!MesosContainerConfig.MESOS_IMAGE_TAGS.keySet().contains(mesosVersion)) {
            throw new RuntimeException("Property 'mesosVersion' supports values: " + StringUtils.join(MesosContainerConfig.MESOS_IMAGE_TAGS.keySet(), ","))
        }
        this.mesosVersion = mesosVersion
    }

    String getLoggingLevel() {
        return loggingLevel
    }
}
