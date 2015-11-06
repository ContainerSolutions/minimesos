package com.containersol.minimesos.mesos;

import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Helper methods for ClusterArchitecture
 */
public class ClusterUtil {
    public static ClusterArchitecture.Builder withSlaves(Integer numberOfSlaves) {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper();
        IntStream.range(0, numberOfSlaves).forEach(x -> builder.withSlave());
        return builder;
    }

    public static <T extends MesosSlave> ClusterArchitecture.Builder withSlaves(Integer numberOfSlaves, Function<ZooKeeper, T> slave) {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper();
        IntStream.range(0, numberOfSlaves).forEach(x -> builder.withSlave(slave::apply));
        return builder;
    }
}
