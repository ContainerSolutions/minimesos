package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import org.apache.log4j.Logger;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.containersol.minimesos.mesos.MesosContainers.*;

/**
 *
 */
public class MesosArchitecture {

    private final MesosContainers mesosContainers = new MesosContainers();

    public MesosContainers getMesosContainers() {
        return mesosContainers;
    }

    public static class Builder {
        private static final Logger LOGGER = Logger.getLogger(MesosArchitecture.class);
        private final MesosArchitecture mesosArchitecture = new MesosArchitecture();
        private final DockerClient dockerClient;

        public Builder() {
            this(DockerClientFactory.build());
        }

        public Builder(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
        }

        public MesosArchitecture build() {
            checkMinimumViableCluster();
            return mesosArchitecture;
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
            return mesosArchitecture.getMesosContainers().isPresent(filter);
        }

        public Builder withZooKeeper() {
            return withZooKeeper(new ZooKeeper(dockerClient));
        }

        public Builder withZooKeeper(ZooKeeper zooKeeper) {
            mesosArchitecture.getMesosContainers().add(zooKeeper); // You don't need a zookeeper container to add a zookeeper container
            return this;
        }

        public Builder withMaster() {
            return withMaster(zooKeeper -> new MesosMaster(dockerClient, zooKeeper));
        }

        public Builder withMaster(Function<ZooKeeper, MesosMaster> master) {
            return withContainer(master::apply);
        }

        public Builder withSlave() {
            return withSlave(zooKeeper -> new MesosSlave(dockerClient, zooKeeper));
        }

        public Builder withSlave(Function<ZooKeeper, MesosSlave> slave) {
            return withContainer(slave::apply);
        }

        public Builder withContainer(Function<ZooKeeper, AbstractContainer> container) {
            return withContainer(container.apply(getZooKeeperContainer()));
        }

        public Builder withContainer(AbstractContainer container) {
            mesosArchitecture.getMesosContainers().add(container); // A simple container may not need zookeeper. But is available if required.
            return this;
        }

        private ZooKeeper getZooKeeperContainer() {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is required by Mesos. You cannot add a Mesos node until you have created a ZooKeeper node. Please add a ZooKeeper node first.");
            }
            return (ZooKeeper) mesosArchitecture.getMesosContainers().getOne(Filter.zooKeeper()).get();
        }
    }

    public static class MesosArchitectureException extends RuntimeException {
        public MesosArchitectureException(String s) {
            super(s);
        }
    }

}