package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import org.apache.log4j.Logger;

import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.containersol.minimesos.mesos.ClusterContainers.*;

/**
 * Represents the cluster architecture in terms of a list of containers. It exposes a builder to help users create a cluster.
 * <p>
 * For example, the simplest cluster you could create is:
 * <p>
 * <code>
 *     new ClusterArchitecture.Builder().build();
 * </code>
 * <p>
 * Which would create a cluster comprising of ZooKeeper, a Mesos Master and a single Mesos slave. This is the minimum viable cluster.
 * These three items are always added if they are not present, because they are required by Mesos. You can add your own simple containers
 * like this (where MyContainer extends from {@link AbstractContainer}):
 * <p><p>
 * <code>
 *     new ClusterArchitecture.Builder().withContainer(new MyContainer(...)).build();
 * </code>
 * <p><p>
 * To add more zookeepers, masters or slaves, you can simply use (note that when adding masters or slaves, zookeeper must be added first, since Mesos requires a link to the ZooKeeper container.):
 * <p>
 * <code>
 *     new ClusterArchitecture.Builder().withZooKeeper().withZooKeeper().withSlave().withSlave().withSlave().build();
 * </code>
 * <p><p>
 * If you need to provide a custom Mesos(Slave/Master) you can provide your own. This will inject a reference to the zooKeeper container, so the Mesos can generate the zkUrl to ZooKeeper.
 * <p>
 * <code>
 *     new ClusterArchitecture.Builder().withZooKeeper().withSlave(zooKeeper -> new MySlave(..., zooKeeper)).build();
 * </code>
 * <p><p>
 * If you want to provide a completely custom container, you can inject an instance of any other container using a filter.
 * Filters are basically instanceof checks. For example, if you needed a reference to the master for some reason:
 * <p>
 * <code>
 *     new ClusterArchitecture.Builder().withContainer(mesosMaster -> new MyContainer(..., mesosMaster), ClusterContainers.Filter.mesosMaster()).build();
 * </code>
 * <p><p>
 * Finally, for the super-leet, you could inject a reference of your own container, into a new container:
 * <p>
 * <code>
 *     new ClusterArchitecture.Builder().withContainer(new MySimpleContainer(...)).withContainer(mySimpleContainer -> new MyInjectedContainer(..., mySimpleContainer), abstractContainer -> abstractContainer instanceof MySimpleContainer).build();
 * </code>
 */
public class ClusterArchitecture {

    private final ClusterContainers clusterContainers = new ClusterContainers();
    public final DockerClient dockerClient;

    public ClusterArchitecture(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public ClusterContainers getClusterContainers() {
        return clusterContainers;
    }

    /**
     * A builder to help construct the cluster. Use the <code>with*</code> methods to add containers.
     */
    public static class Builder {
        private static final Logger LOGGER = Logger.getLogger(ClusterArchitecture.class);
        private final ClusterArchitecture clusterArchitecture;
        private final DockerClient dockerClient;

        /**
         * Create a new builder with the default docker client
         */
        public Builder() {
            this(DockerClientFactory.build());
        }

        /**
         * Create a new builder with the provided docker client
         */
        public Builder(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
            this.clusterArchitecture = new ClusterArchitecture(dockerClient);
        }

        /**
         * Return the built cluster architecture. Will check that it creates at least a minimum viable cluster, and if not it will add the missing containers.
         * @return the {@link ClusterArchitecture}
         */
        public ClusterArchitecture build() {
            checkMinimumViableCluster();
            return clusterArchitecture;
        }

        /**
         * Return the containers currently in the cluster. Note that this method is not checked. If you want to start containers, use {@see Builder.build()}
         * @return List of containers wrapped in a {@link ClusterContainers} object
         */
        public ClusterContainers getContainers() {
            return clusterArchitecture.getClusterContainers();
        }

        /**
         * Includes the default {@link ZooKeeper} instance in the cluster
         */
        public Builder withZooKeeper() {
            return withZooKeeper(new ZooKeeper(dockerClient));
        }

        /**
         * Provide a custom implementation of the {@link ZooKeeper} container.
         * @param zooKeeper must extend from {@link ZooKeeper}
         */
        public Builder withZooKeeper(ZooKeeper zooKeeper) {
            getContainers().add(zooKeeper); // You don't need a zookeeper container to add a zookeeper container
            return this;
        }

        /**
         * Includes the default {@link MesosMaster} instance in the cluster
         */
        public Builder withMaster() {
            return withMaster(zooKeeper -> new MesosMaster(dockerClient, zooKeeper));
        }

        /**
         * Provide a custom implementation of the {@link MesosMaster} container.
         * @param master must extend from {@link MesosMaster}. Functional, to allow you to inject a reference to the {@link ZooKeeper} container.
         */
        public Builder withMaster(Function<ZooKeeper, MesosMaster> master) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos node until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(master::apply, Filter.zooKeeper());
        }

        /**
         * Includes the default {@link MesosSlave} instance in the cluster
         */
        public Builder withSlave() {
            return withSlave(zooKeeper -> new MesosSlave(dockerClient, zooKeeper));
        }

        /**
         * All default instance, but with defined resources
         * @param resources definition of resources
         */
        public Builder withSlave(String resources) {
            return withSlave(zooKeeper -> new MesosSlave(dockerClient, zooKeeper) {
                @Override
                public TreeMap<String, String> getDefaultEnvVars() {
                    TreeMap<String, String> envVars = super.getDefaultEnvVars();
                    envVars.put("MESOS_RESOURCES", resources);
                    return envVars;
                }
            });
        }

        /**
         * Provide a custom implementation of the {@link MesosSlave} container.
         * @param slave must extend from {@link MesosSlave}. Functional, to allow you to inject a reference to the {@link ZooKeeper} container.
         */
        public Builder withSlave(Function<ZooKeeper, MesosSlave> slave) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos node until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(slave::apply, Filter.zooKeeper());
        }

        /**
         * Includes your own container in the cluster
         * @param container must extend from {@link AbstractContainer}
         */
        public Builder withContainer(AbstractContainer container) {
            getContainers().add(container); // A simple container may not need any injection. But is available if required.
            return this;
        }

        /**
         * Includes your own container in the cluster with a reference to another container of type T
         * @param container container must extend from {@link AbstractContainer}. Functional, to allow you to inject a reference to another {@link AbstractContainer}.
         * @param filter A predicate that returns true if the {@link AbstractContainer} is of type T
         */
        public Builder withContainer(Function<ZooKeeper, AbstractContainer> container, Predicate<AbstractContainer> filter) {
            // Dev note: It is not possible to use generics to find the requested type due to generic type erasure. This is why we are explicitly passing a user provided filter.
            Optional<ZooKeeper> foundContainer = getContainers().getOne(filter);
            if (!foundContainer.isPresent()) {
                throw new MesosArchitectureException("Could not find a container of that type when trying to inject.");
            }
            return withContainer(container.apply(foundContainer.get()));
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
            return getContainers().isPresent(filter);
        }

    }

    /**
     * Thrown when there is something wrong with the cluster architecture
     */
    public static class MesosArchitectureException extends RuntimeException {
        public MesosArchitectureException(String s) {
            super(s);
        }
    }

}
