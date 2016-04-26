package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.ZooKeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.containersol.minimesos.cluster.Filter.withRole;

/**
 * Helper methods for ClusterArchitecture
 */
public class ClusterUtil {

    /**
     * Disable constructors
     */
    private ClusterUtil() {
    }

    public static ClusterArchitecture.Builder withAgent(Integer numberOfAgents) {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper();
        IntStream.range(0, numberOfAgents).forEach(x -> builder.withAgent());
        return builder;
    }

    public static <T extends MesosAgent> ClusterArchitecture.Builder withAgent(Integer numberOfAgents, Function<ZooKeeper, T> agent) {
        ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder().withZooKeeper();
        IntStream.range(0, numberOfAgents).forEach(x -> builder.withAgent(agent::apply));
        return builder;
    }

    /**
     * Filters given list of processes and returns only those with distinct roles
     *
     * @param processes complete list of processes
     * @return processes with distinct roles
     */
    public static List<ClusterProcess> getDistinctRoleProcesses(List<ClusterProcess> processes) {

        List<ClusterProcess> distinct = new ArrayList<>();
        Map<String, Integer> roles = new HashMap<>();

        // count processes per role
        for (ClusterProcess process : processes) {
            Integer prev = roles.get(process.getRole());
            int count = (prev != null) ? prev : 0;
            roles.put(process.getRole(), count+1 );
        }

        for (Map.Entry<String, Integer> role : roles.entrySet() ) {
            if (role.getValue() == 1) {
                Optional<ClusterProcess> process = processes.stream().filter(withRole(role.getKey())).findFirst();
                distinct.add(process.get());
            }
        }

        return distinct;

    }

}
