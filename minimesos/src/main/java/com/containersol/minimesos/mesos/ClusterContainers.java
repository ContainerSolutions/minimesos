package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Holds the containers and helper methods for the Mesos cluster
 */
public class ClusterContainers {

    private final List<ClusterMember> containers;

    /**
     * Create a container List from scratch
     */
    public ClusterContainers() {
        containers = new ArrayList<>();
    }

    /**
     * Create a container List from another List
     * @param containers another List of {@link ClusterMember}
     */
    public ClusterContainers(List<ClusterMember> containers) {
        this.containers = containers;
    }

    /**
     * Add a container to the list of containers.
     * @param container of type {@link ClusterMember}
     * @return this, for fluent adding.
     */
    public ClusterContainers add(ClusterMember container) {
        containers.add(container);
        return this;
    }

    public List<ClusterMember> getContainers() {
        return containers;
    }

    /**
     * Optionally get one of a certain type of type T. Note, this cast will always work because we are filtering on that type.
     * If it doesn't find that type, the optional is empty so the cast doesn't need to be performed.
     *
     * @param filter A predicate that is true when an {@link ClusterMember} in the list is of type T
     * @param <T> A container of type T that extends {@link ClusterMember}
     * @return the first container it comes across.
     */
    @SuppressWarnings("unchecked")
    public <T extends ClusterMember> Optional<T> getOne(Predicate<ClusterMember> filter) {
        return (Optional<T>) getContainers().stream().filter(filter).findFirst();
    }

    /**
     * Checks to see whether a container exists
     * @param filter A predicate that is true when an {@link ClusterMember} in the list is of type T
     * @return true if it exists
     */
    public Boolean isPresent(Predicate<ClusterMember> filter) {
        return getOne(filter).isPresent();
    }

}
