package com.containersol.minimesos.cluster;

import java.util.function.Predicate;

public class Filter {

    private Filter() {
    }

    public static Predicate<ClusterProcess> zooKeeper() {
        return process -> process instanceof ZooKeeper;
    }

    public static Predicate<ClusterProcess> consul() {
        return process -> process instanceof Consul;
    }

    public static Predicate<ClusterProcess> mesosMaster() {
        return process -> process instanceof MesosMaster;
    }

    public static Predicate<ClusterProcess> mesosAgent() {
        return process -> process instanceof MesosAgent;
    }

    public static Predicate<ClusterProcess> marathon() {
        return process -> process instanceof Marathon;
    }

    public static Predicate<ClusterProcess> withRole(String role) {
        return process -> role.equals(process.getRole());
    }

    public static Predicate<ClusterProcess> registrator() { return process -> process instanceof Registrator; }

    public static Predicate<ClusterProcess> mesosDns() {
        return process -> process instanceof MesosDns;
    }
}
