package com.containersol.minimesos.cluster;

import java.util.function.Predicate;

public class Filter {
    public static Predicate<ClusterProcess> zooKeeper() {
        return abstractContainer -> abstractContainer instanceof ZooKeeper;
    }

    public static Predicate<ClusterProcess> consul() {
        return abstractContainer -> abstractContainer instanceof Consul;
    }

    public static Predicate<ClusterProcess> mesosMaster() {
        return abstractContainer -> abstractContainer instanceof MesosMaster;
    }

    public static Predicate<ClusterProcess> mesosAgent() {
        return abstractContainer -> abstractContainer instanceof MesosAgent;
    }

    public static Predicate<ClusterProcess> marathon() {
        return abstractContainer -> abstractContainer instanceof Marathon;
    }
}
