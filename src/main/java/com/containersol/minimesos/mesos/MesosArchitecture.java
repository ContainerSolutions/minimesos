package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import org.apache.log4j.Logger;

import java.util.function.Predicate;

import static com.containersol.minimesos.mesos.MesosContainers.*;

/**
 *
 */
public class MesosArchitecture {

    private static final String DEFAULT_MESOS_ZK_PATH = "/mesos";
    private final MesosContainers mesosContainers = new MesosContainers();

    public MesosContainers getMesosContainers() {
        return mesosContainers;
    }

    public static class Builder {
        private static final Logger LOGGER = Logger.getLogger(MesosArchitecture.class);
        public static final int DEFAULT_ZOOKEEPER_PORT = 2181;
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

        private String generateZKUrl() {
            if (!isPresent(Filter.zooKeeper())) {
                throw new MesosArchitectureException("ZooKeeper is not yet added to the cluster. Mesos requires ZooKeeper. Please add ZooKeeper first.");
            }
            String zkIp = mesosArchitecture.getMesosContainers().getOne(Filter.zooKeeper()).get().getIpAddress();
            return "zk://" + zkIp + ":" + DEFAULT_ZOOKEEPER_PORT + "/" + DEFAULT_MESOS_ZK_PATH;
        }

        public Builder withZooKeeper() {
            return withContainer(new ZooKeeper(dockerClient));
        }

        public Builder withMaster() {
            return withContainer(new MesosMaster(dockerClient, generateZKUrl()));
        }

        public Builder withMaster(MesosMaster customMaster) {
            return withContainer(customMaster);
        }

        public Builder withSlave() {
            return withContainer(new MesosSlave(dockerClient, generateZKUrl()));
        }

        public Builder withSlave(MesosSlave customSlave) {
            return withContainer(customSlave);
        }

        public Builder withContainer(AbstractContainer container) {
            mesosArchitecture.getMesosContainers().add(container);
            return this;
        }
    }

    public static class MesosArchitectureException extends RuntimeException {
        public MesosArchitectureException(String s) {
            super(s);
        }
    }

}
