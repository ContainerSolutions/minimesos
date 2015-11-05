package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import org.apache.log4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.containersol.minimesos.mesos.ClusterContainers.*;

/**
 *
 */
public class ClusterArchitecture {

    private final ClusterContainers clusterContainers = new ClusterContainers();

    public ClusterContainers getClusterContainers() {
        return clusterContainers;
    }

    public static class Builder {
        private static final Logger LOGGER = Logger.getLogger(ClusterArchitecture.class);
        private final ClusterArchitecture clusterArchitecture = new ClusterArchitecture();
        private final DockerClient dockerClient;

        public Builder() {
            this(DockerClientFactory.build());
        }

        public Builder(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        public ClusterArchitecture build() {
            checkMinimumViableCluster();
            return clusterArchitecture;
        }

        private void checkMinimumViableCluster() {
            if (!isPresent(Filter.zooKeeper())) { // Must check for zk first, as it is required by the master
                LOGGER.info("Instance of ZooKeeper not found. Adding ZooKeeper.");
                withZooKeeper();
            }
            if (!isPresent(Filter.mesosMaster())) {
                LOGGER.info("Instance of MesosMaster not found. Adding MesosMaster.");
                withMaster();
            }
            if (!isPresent(Filter.mesosSlave())) {
                LOGGER.info("Instance of MesosSlave not found. Adding MesosSlave.");
                withSlave();
            }
        }

        private Boolean isPresent(Predicate<AbstractContainer> filter) {
            return clusterArchitecture.getClusterContainers().isPresent(filter);
        }

        public Builder withZooKeeper() {
            return withZooKeeper(new ZooKeeper(dockerClient));
        }

        public Builder withZooKeeper(ZooKeeper zooKeeper) {
            clusterArchitecture.getClusterContainers().add(zooKeeper); // You don't need a zookeeper container to add a zookeeper container
            return this;
        }

        public Builder withMaster() {
            return withMaster(zooKeeper -> new MesosMaster(dockerClient, zooKeeper));
        }

        public Builder withMaster(Function<ZooKeeper, MesosMaster> master) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos node until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(master::apply, Filter.zooKeeper());
        }

        public Builder withSlave() {
            return withSlave(zooKeeper -> new MesosSlave(dockerClient, zooKeeper));
        }

        public Builder withSlave(Function<ZooKeeper, MesosSlave> slave) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos node until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(slave::apply, Filter.zooKeeper());
        }

        public Builder withContainer(Function<ZooKeeper, AbstractContainer> container, Predicate<AbstractContainer> filter) {
            // Dev note: It is not possible to use generics to find the requested type due to generic type erasure. This is why we are explicitly passing a user provided filter.
            Optional<ZooKeeper> foundContainer = clusterArchitecture.getClusterContainers().getOne(filter);
            if (!foundContainer.isPresent()) {
                throw new MesosArchitectureException("Could not find a container of that type when trying to inject.");
            }
            return withContainer(container.apply(foundContainer.get()));
        }

        public Builder withContainer(AbstractContainer container) {
            clusterArchitecture.getClusterContainers().add(container); // A simple container may not need any injection. But is available if required.
            return this;
        }
    }

    public static class MesosArchitectureException extends RuntimeException {
        public MesosArchitectureException(String s) {
            super(s);
        }
    }

}
