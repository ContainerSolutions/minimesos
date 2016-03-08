package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.marathon.Marathon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Holds the containers and helper methods for the Mesos cluster
 */
public class ClusterContainers {

    private final List<AbstractContainer> containers;

    /**
     * Create a container List from scratch
     */
    public ClusterContainers() {
        containers = new ArrayList<>();
    }

    /**
     * Create a container List from another List
     * @param containers another List of {@link AbstractContainer}
     */
    public ClusterContainers(List<AbstractContainer> containers) {
        this.containers = containers;
    }

    /**
     * Add a container to the list of containers.
     * @param container of type {@link AbstractContainer}
     * @return this, for fluent adding.
     */
    public ClusterContainers add(AbstractContainer container) {
        containers.add(container);
        return this;
    }

    public List<AbstractContainer> getContainers() {
        return containers;
    }

    /**
     * Optionally get one of a certain type of type T. Note, this cast will always work because we are filtering on that type.
     * If it doesn't find that type, the optional is empty so the cast doesn't need to be performed.
     *
     * @param filter A predicate that is true when an {@link AbstractContainer} in the list is of type T
     * @param <T> A container of type T that extends {@link AbstractContainer}
     * @return the first container it comes across.
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractContainer> Optional<T> getOne(Predicate<AbstractContainer> filter) {
        return (Optional<T>) getContainers().stream().filter(filter).findFirst();
    }

    /**
     * Checks to see whether a container exists
     * @param filter A predicate that is true when an {@link AbstractContainer} in the list is of type T
     * @return true if it exists
     */
    public Boolean isPresent(Predicate<AbstractContainer> filter) {
        return getOne(filter).isPresent();
    }

    public static class Filter {
        public static Predicate<AbstractContainer> zooKeeper() {
            return abstractContainer -> abstractContainer instanceof ZooKeeper;
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

}
