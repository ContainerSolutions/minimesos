package com.containersol.minimesos.config

import groovy.util.logging.Slf4j
import org.apache.commons.lang.StringUtils

@Slf4j
class ClusterConfig extends GroovyBlock {

    def call(Closure cl) {
        cl.setDelegate(this);
        cl.setResolveStrategy(Closure.DELEGATE_ONLY)
        cl.call();
    }

    boolean exposePorts = true
    int timeout = 60
    String mesosVersion = "0.25"
    def clusterName = "minimesos"
    String loggingLevel = "INFO"

    MesosMasterConfig master = null
    List<MesosAgentConfig> agents = new ArrayList<>()
    ZooKeeperConfig zookeeper = null
    MarathonConfig marathon = null
    ConsulConfig consul = null

    def master(@DelegatesTo(MesosMasterConfig) Closure cl) {
        if (master != null) {
            throw new RuntimeException("Multiple Masters are not supported in this version yet")
        }
        master = new MesosMasterConfig();
        master.setLoggingLevel(loggingLevel)
        delegateTo(master, cl)
    }

    def agent(@DelegatesTo(MesosAgentConfig) Closure cl) {
        def agent = new MesosAgentConfig()
        agent.setLoggingLevel(getLoggingLevel())
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
        this.loggingLevel = loggingLevel
    }

    String getLoggingLevel() {
        return loggingLevel
    }
}
