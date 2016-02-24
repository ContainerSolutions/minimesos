package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class ClusterConfig extends GroovyBlock {

    def call(Closure cl) {
        cl.setDelegate(this);
        cl.setResolveStrategy(Closure.DELEGATE_ONLY)
        cl.call();
    }

    def exposePorts = true
    int timeout = 60
    String mesosVersion = "0.25"

    def clusterName = "minimesos-test"

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

}
