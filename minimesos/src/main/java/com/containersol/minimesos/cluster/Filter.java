package com.containersol.minimesos.cluster;

import java.util.function.Predicate;

public class Filter {
    public static Predicate<AbstractContainer> zooKeeper() {
        return abstractContainer -> abstractContainer instanceof ZooKeeper;
    }

    public static Predicate<AbstractContainer> consul() {
        return abstractContainer -> abstractContainer instanceof Consul;
    }

    public static Predicate<AbstractContainer> mesosMaster() {
        return abstractContainer -> abstractContainer instanceof MesosMaster;
    }

    public static Predicate<AbstractContainer> mesosAgent() {
        return abstractContainer -> abstractContainer instanceof MesosAgent;
    }

    public static Predicate<AbstractContainer> marathon() {
        return abstractContainer -> abstractContainer instanceof Marathon;
    }
}
