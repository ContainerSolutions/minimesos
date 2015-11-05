package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Holds the containers and helper methods for the Mesos cluster
 */
public class MesosContainers {
    private final List<AbstractContainer> containers;

    public MesosContainers() {
        containers = new ArrayList<>();
    }

    public MesosContainers(List<AbstractContainer> containers) {
        this.containers = containers;
    }

    public MesosContainers add(AbstractContainer container) {
        containers.add(container);
        return this;
    }

    public List<AbstractContainer> getContainers() {
        return containers;
    }

    public <T extends AbstractContainer> Optional<T> getOne(Predicate<AbstractContainer> filter) {
        return (Optional<T>) getContainers().stream().filter(filter).findAny();
    }

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

        public static Predicate<AbstractContainer> mesosSlave() {
            return abstractContainer -> abstractContainer instanceof MesosSlave;
        }
    }

}
