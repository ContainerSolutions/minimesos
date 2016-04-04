package com.containersol.minimesos.cluster;

import java.util.function.Predicate;

public class Filter {
    public static Predicate<ClusterMember> zooKeeper() {
        return abstractContainer -> abstractContainer instanceof ZooKeeper;
    }

    public static Predicate<ClusterMember> consul() {
        return abstractContainer -> abstractContainer instanceof Consul;
    }

    public static Predicate<ClusterMember> mesosMaster() {
        return abstractContainer -> abstractContainer instanceof MesosMaster;
    }

    public static Predicate<ClusterMember> mesosAgent() {
        return abstractContainer -> abstractContainer instanceof MesosAgent;
    }

    public static Predicate<ClusterMember> marathon() {
        return abstractContainer -> abstractContainer instanceof Marathon;
    }
}
