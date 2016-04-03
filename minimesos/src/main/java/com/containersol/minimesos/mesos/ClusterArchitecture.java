package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.*;
import com.containersol.minimesos.config.*;
import com.containersol.minimesos.container.AbstractContainerImpl;
import com.containersol.minimesos.marathon.MarathonContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents the cluster architecture in terms of a list of containers. It exposes a builder to help users create a cluster.
 * <p>
 * For example, the simplest cluster you could create is:
 * <p>
 * <code>
 * new ClusterArchitecture.Builder().build();
 * </code>
 * <p>
 * Which would create a cluster comprising of ZooKeeper, a Mesos Master and a single Mesos agent. This is the minimum viable cluster.
 * These three items are always added if they are not present, because they are required by Mesos. You can add your own simple containers
 * like this (where MyContainer extends from {@link AbstractContainerImpl}):
 * <p><p>
 * <code>
 * new ClusterArchitecture.Builder().withContainer(new MyContainer(...)).build();
 * </code>
 * <p><p>
 * To add more zookeepers, masters or agents, you can simply use (note that when adding masters or agents, zookeeper must be added first, since Mesos requires a link to the ZooKeeper container.):
 * <p>
 * <code>
 * new ClusterArchitecture.Builder().withZooKeeper().withZooKeeper().withAgent().withAgent().withAgent().build();
 * </code>
 * <p><p>
 * If you need to provide a custom Mesos(Agent/Master) you can provide your own. This will inject a reference to the zooKeeper container, so the Mesos can generate the zkUrl to ZooKeeper.
 * <p>
 * <code>
 * new ClusterArchitecture.Builder().withZooKeeper().withAgent(zooKeeper -> new MyAgent(..., zooKeeper)).build();
 * </code>
 * <p><p>
 * If you want to provide a completely custom container, you can inject an instance of any other container using a filter.
 * Filters are basically instanceof checks. For example, if you needed a reference to the master for some reason:
 * <p>
 * <code>
 * new ClusterArchitecture.Builder().withContainer(mesosMaster -> new MyContainer(..., mesosMaster), ClusterContainers.Filter.mesosMaster()).build();
 * </code>
 * <p><p>
 * Finally, for the super-leet, you could inject a reference of your own container, into a new container:
 * <p>
 * <code>
 * new ClusterArchitecture.Builder().withContainer(new MySimpleContainer(...)).withContainer(mySimpleContainer -> new MyInjectedContainer(..., mySimpleContainer), abstractContainer -> abstractContainer instanceof MySimpleContainer).build();
 * </code>
 */
public class ClusterArchitecture {

    private final ClusterContainers clusterContainers = new ClusterContainers();
    private final ClusterConfig clusterConfig;

    public ClusterArchitecture() {
        this.clusterConfig = new ClusterConfig();
    }

    private ClusterArchitecture(ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public ClusterContainers getClusterContainers() {
        return clusterContainers;
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    /**
     * A builder to help construct the cluster. Use the <code>with*</code> methods to add containers.
     */
    public static class Builder {

        private static final Logger LOGGER = LoggerFactory.getLogger(ClusterArchitecture.class);

        private final ClusterArchitecture clusterArchitecture;

        /**
         * Create a new builder with the provided docker client
         */
        public Builder() {
            this.clusterArchitecture = new ClusterArchitecture();
        }

        private Builder(ClusterConfig clusterConfig) {
            this.clusterArchitecture = new ClusterArchitecture(clusterConfig);
        }

        /**
         * Creates architecture for default cluster configuration
         *
         * @param clusterConfig loaded from file cluster configuration
         * @return reference to the given builder, so the method call can be chained
         */
        static public ClusterArchitecture.Builder createCluster(ClusterConfig clusterConfig) {
            Builder configBuilder = new Builder(clusterConfig);

            configBuilder.withZooKeeper(clusterConfig.getZookeeper());
            configBuilder.withMaster(zooKeeper -> new MesosMasterContainer(zooKeeper, clusterConfig.getMaster()));

            // creation of agents
            List<MesosAgentConfig> agentConfigs = clusterConfig.getAgents();
            for (MesosAgentConfig agentConfig : agentConfigs) {
                configBuilder.withAgent(zooKeeper -> new MesosAgentContainer(zooKeeper, agentConfig));
            }

            // Marathon (optional)
            if (clusterConfig.getMarathon() != null) {
                configBuilder.withMarathon(zooKeeper -> new MarathonContainer(zooKeeper, clusterConfig.getMarathon()));
            }

            // Consul (optional)
            ConsulConfig consulConfig = clusterConfig.getConsul();
            if (consulConfig != null) {
                configBuilder.withConsul(new ConsulContainer(consulConfig));
            }

            // Registrator (optional)
            RegistratorConfig registratorConfig = clusterConfig.getRegistrator();
            if (registratorConfig != null) {
                configBuilder.withRegistrator(consul -> new RegistratorContainer(consul, registratorConfig));
            }

            return configBuilder;
        }

        /**
         * Return the built cluster architecture. Will check that it creates at least a minimum viable cluster, and if not it will add the missing containers.
         *
         * @return the {@link ClusterArchitecture}
         */
        public ClusterArchitecture build() {
            checkMinimumViableCluster();
            return clusterArchitecture;
        }

        /**
         * Return the containers currently in the cluster. Note that this method is not checked. If you want to start containers, use {@see Builder.build()}
         *
         * @return List of containers wrapped in a {@link ClusterContainers} object
         */
        public ClusterContainers getContainers() {
            return clusterArchitecture.getClusterContainers();
        }

        /**
         * Includes the default {@link ZooKeeperContainer} instance in the cluster
         */
        public Builder withZooKeeper() {
            return withZooKeeper(new ZooKeeperConfig());
        }

        /**
         * Be explicit about the version of the image to use.
         */
        public Builder withZooKeeper(ZooKeeperConfig zooKeeperConfig) {
            return withZooKeeper(new ZooKeeperContainer(zooKeeperConfig));
        }

        /**
         * Provide a custom implementation of the {@link ZooKeeper} container.
         *
         * @param zooKeeper must extend from {@link ZooKeeper}
         */
        public Builder withZooKeeper(ZooKeeper zooKeeper) {
            getContainers().add(zooKeeper); // You don't need a zookeeper container to add a zookeeper container
            return this;
        }

        public Builder withConsul(Consul consul) {
            getContainers().add(consul);
            return this;
        }

        public Builder withRegistrator(Registrator registrator) {
            getContainers().add(registrator);
            return this;
        }

        /**
         * Includes the default {@link MesosMaster} instance in the cluster
         */
        public Builder withMaster() {
            return withMaster(MesosMasterContainer::new);
        }

        /**
         * Includes the default {@link MesosAgent} instance in the cluster
         */
        public Builder withAgent() {
            return withAgent(MesosAgentContainer::new);
        }

        /**
         * All default instance, but with defined resources
         *
         * @param agentResourcesConfig agent resources configuration
         */
        public Builder withAgent(String agentResourcesConfig) {
            AgentResourcesConfig resources = AgentResourcesConfig.fromString(agentResourcesConfig);
            MesosAgentConfig config = new MesosAgentConfig();
            config.setResources(resources);
            return withAgent(zooKeeper -> new MesosAgentContainer(zooKeeper, config));
        }

        /**
         * Provide a custom implementation of the {@link MesosMaster} container.
         *
         * @param master must extend from {@link MesosMaster}. Functional, to allow you to inject a reference to the {@link ZooKeeper} container.
         */
        public Builder withMaster(Function<ZooKeeper, MesosMaster> master) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos node until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(master::apply, Filter.zooKeeper());
        }

        public Builder withRegistrator(Function<Consul, Registrator> registrator) {
            if (!isPresent(Filter.consul())) {
                throw new MesosArchitectureException("Consul is required by Registrator. You cannot add a Registrator node until you have created a Consul node. Please add a Consul node first.");
            }
            return withContainer(registrator::apply, Filter.consul());
        }

        /**
         * Provide a custom implementation of the {@link MesosAgent} container.
         *
         * @param agent must extend from {@link MesosAgent}. Functional, to allow you to inject a reference to the {@link ZooKeeper} container.
         */
        public Builder withAgent(Function<ZooKeeper, MesosAgent> agent) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos agent until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(agent::apply, Filter.zooKeeper());
        }

        public Builder withMarathon(Function<ZooKeeper, Marathon> marathon) {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos agent until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return withContainer(marathon::apply, Filter.zooKeeper());
        }

        /**
         * Includes your own container in the cluster
         *
         * @param container must extend from {@link AbstractContainer}
         */
        public Builder withContainer(AbstractContainer container) {
            getContainers().add(container); // A simple container may not need any injection. But is available if required.
            return this;
        }

        /**
         * Includes your own container in the cluster with a reference to another container of type T
         *
         * @param container container must extend from {@link AbstractContainer}. Functional, to allow you to inject a reference to another {@link AbstractContainer}.
         * @param filter    A predicate that returns true if the {@link AbstractContainer} is of type T
         */
        public <T extends AbstractContainer> Builder withContainer(Function<T, AbstractContainer> container, Predicate<AbstractContainer> filter) {
            // Dev note: It is not possible to use generics to find the requested type due to generic type erasure. This is why we are explicitly passing a user provided filter.
            Optional<T> foundContainer = getContainers().getOne(filter);
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
            if (!isPresent(Filter.mesosAgent())) {
                LOGGER.info("Instance of MesosAgent not found. Adding MesosAgent.");
                withAgent();
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
