package com.containersol.minimesos.config

import groovy.util.logging.Slf4j

@Slf4j
class Cluster extends Block {

    def call(Closure cl) {
        cl.setDelegate(this);
        cl.setResolveStrategy(Closure.DELEGATE_ONLY)
        cl.call();
    }

    def exposePorts = true
    int timeout = 60
    String mesosVersion = "0.25"
    def loggingLevel = "INFO"
    def clusterName = "minimesos-test"

    Master master = null
    List<Agent> agents = new ArrayList<>()
    Zookeeper zookeeper = null
    Marathon marathon = null

    def master(@DelegatesTo(Agent) Closure cl) {
        if (master != null) {
            throw new RuntimeException("Multiple Masters are not supported in this version yet")
        }
        master = new Master();
        delegateTo(master, cl)
    }

    def agent(@DelegatesTo(Agent) Closure cl) {
        def agent = new Agent()
        delegateTo(agent, cl)
        agents.add(agent)
    }

    def zookeeper(@DelegatesTo(Zookeeper) Closure cl) {
        if (zookeeper != null) {
            throw new RuntimeException("Multiple Zookeepers are not supported in this version yet")
        }
        zookeeper = new Zookeeper();
        delegateTo(zookeeper, cl)
    }

    def marathon(@DelegatesTo(Marathon) Closure cl) {
        if (marathon != null) {
            throw new RuntimeException("Cannot have more than 1 marathon")
        }
        marathon = new Marathon();
        delegateTo(marathon, cl)
    }


}
