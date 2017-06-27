package com.containersol.minimesos.mesos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.Consul;
import com.containersol.minimesos.cluster.Filter;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.cluster.MesosDns;
import com.containersol.minimesos.cluster.MesosMaster;
import com.containersol.minimesos.cluster.Registrator;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.integrationtest.container.ContainerName;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.MarathonContainer;
import com.github.dockerjava.api.model.Container;

import com.github.dockerjava.api.model.ContainerPort;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker based factory of minimesos cluster members
 */
public class MesosClusterContainersFactory extends MesosClusterFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MesosClusterContainersFactory.class);

    public ZooKeeper createZooKeeper(MesosCluster mesosCluster, String uuid, String containerId) {
        return new ZooKeeperContainer(mesosCluster, uuid, containerId);
    }

    public MesosAgent createMesosAgent(MesosCluster mesosCluster, String uuid, String containerId) {
        return new MesosAgentContainer(mesosCluster, uuid, containerId);
    }

    public MesosMaster createMesosMaster(MesosCluster mesosCluster, String uuid, String containerId) {
        return new MesosMasterContainer(mesosCluster, uuid, containerId);
    }

    public Marathon createMarathon(MesosCluster mesosCluster, String uuid, String containerId) {
        return new MarathonContainer(mesosCluster, uuid, containerId);
    }

    public Consul createConsul(MesosCluster mesosCluster, String uuid, String containerId) {
        return new ConsulContainer(mesosCluster, uuid, containerId);
    }

    public Registrator createRegistrator(MesosCluster mesosCluster, String uuid, String containerId) {
        return new RegistratorContainer(mesosCluster, uuid, containerId);
    }

    public MesosDns createMesosDns(MesosCluster cluster, String uuid, String containerId) {
        return new MesosDnsContainer(cluster, uuid, containerId);
    }

    @Override
    public void loadRunningCluster(MesosCluster cluster) {
        String clusterId = cluster.getClusterId();
        List<ClusterProcess> containers = cluster.getMemberProcesses();

        List<Container> dockerContainers = DockerContainersUtil.getContainers(false).getContainers();
        dockerContainers.sort(Comparator.comparingLong(Container::getCreated));

        for (Container container : dockerContainers) {
            String name = ContainerName.getFromDockerNames(container.getNames());
            if (ContainerName.belongsToCluster(name, clusterId)) {

                String containerId = container.getId();

                String[] parts = name.split("-");
                if (parts.length > 3) {

                    String role = parts[1];
                    String uuid = parts[3];

                    switch (role) {
                        case "zookeeper":
                            containers.add(createZooKeeper(cluster, uuid, containerId));
                            break;
                        case "agent":
                            containers.add(createMesosAgent(cluster, uuid, containerId));
                            break;
                        case "master":
                            MesosMaster master = createMesosMaster(cluster, uuid, containerId);
                            containers.add(master);

                            restoreMapToPorts(cluster, container);
                            break;
                        case "marathon":
                            containers.add(createMarathon(cluster, uuid, containerId));
                            break;
                        case "consul":
                            containers.add(createConsul(cluster, uuid, containerId));
                            break;
                        case "registrator":
                            containers.add(createRegistrator(cluster, uuid, containerId));
                            break;
                        case "mesosdns":
                            containers.add(createMesosDns(cluster, uuid, containerId));
                    }
                }
            }
        }
    }

    private void restoreMapToPorts(MesosCluster cluster, Container container) {
        // Restore "map ports to host" attribute
        ContainerPort[] ports = container.getPorts();
        if (ports != null) {
            for (ContainerPort port : ports) {
                if (port.getIp() != null && port.getPrivatePort() == MesosMasterConfig.MESOS_MASTER_PORT) {
                    cluster.setMapPortsToHost(true);
                }
            }
        }
    }

    @Override
    public void destroyRunningCluster(String clusterId) {
        DockerContainersUtil.getContainers(true).filterByName(ContainerName.getContainerNamePattern(clusterId)).kill(true).remove();
    }

    public MesosCluster createMesosCluster(String path) {
        try (InputStream is = new FileInputStream(path)) {
            return createMesosCluster(is);
        } catch (IOException e) {
            LOGGER.debug("Could not read minimesos config: ", e.getMessage());
            throw new MinimesosException("Could not read minimesos config: " + e.getMessage());
        }
    }

    public MesosCluster createMesosCluster(InputStream inputStream) {
        try {
            ClusterConfig clusterConfig = new ConfigParser().parse(IOUtils.toString(inputStream, "UTF-8"));
            return createMesosCluster(clusterConfig);
        } catch (IOException e) {
            throw new MinimesosException("Could not read minimesos config:" + e.getCause());
        }
    }

    public MesosCluster createMesosCluster(ClusterConfig clusterConfig) {
        LOGGER.debug("Creating Mesos cluster");


        ClusterContainers clusterContainers = createProcesses(clusterConfig);

        validateProcesses(clusterContainers);

        connectProcesses(clusterContainers);

        return new MesosCluster(clusterConfig, clusterContainers.getContainers());
    }

    private static ClusterContainers createProcesses(ClusterConfig clusterConfig) {
        LOGGER.debug("Creating cluster processes");

        ClusterContainers clusterContainers = new ClusterContainers();

        ZooKeeperContainer zooKeeper = new ZooKeeperContainer(clusterConfig.getZookeeper());
        clusterContainers.add(zooKeeper);

        if (clusterConfig.getMesosdns() != null) {
            clusterContainers.add(new MesosDnsContainer(clusterConfig.getMesosdns()));
        }

        MesosMasterContainer mesosMaster = new MesosMasterContainer(clusterConfig.getMaster());
        clusterContainers.add(mesosMaster);

        if (clusterConfig.getMarathon() != null) {
            clusterContainers.add(new MarathonContainer(clusterConfig.getMarathon()));
        }

        clusterConfig.getAgents().forEach(config -> clusterContainers.add(new MesosAgentContainer(config)));

        if (clusterConfig.getConsul() != null) {
            clusterContainers.add(new ConsulContainer(clusterConfig.getConsul()));
        }

        if (clusterConfig.getRegistrator() != null) {
            clusterContainers.add(new RegistratorContainer(clusterConfig.getRegistrator()));
        }

        return clusterContainers;
    }

    private static void validateProcesses(ClusterContainers clusterContainers) {
        LOGGER.debug("Validating cluster processes");

        if (!isPresent(clusterContainers, Filter.mesosMaster())) {
            throw new MinimesosException("Cluster requires a single Mesos Master. Please add one in the minimesosFile.");
        }

        if (!isPresent(clusterContainers, Filter.zooKeeper())) {
            throw new MinimesosException("Cluster requires a single ZooKeeper. Please add one in the minimesosFile.");
        }

        if (!isPresent(clusterContainers, Filter.mesosAgent())) {
            throw new MinimesosException("Cluster requires at least 1 Mesos Agent. Please add one in the minimesosFile.");
        }

        if (isPresent(clusterContainers, Filter.registrator()) && !isPresent(clusterContainers, Filter.consul())) {
            throw new MinimesosException("Registrator requires a single Consul. Please add consul in the minimesosFile.");
        }
    }

    private static void connectProcesses(ClusterContainers clusterContainers) {
        LOGGER.debug("Connecting cluster processes");

        ZooKeeper zookeeper = (ZooKeeper) clusterContainers.getOne(Filter.zooKeeper()).get();
        MesosMaster mesosMaster = (MesosMaster) clusterContainers.getOne(Filter.mesosMaster()).get();
        mesosMaster.setZooKeeper(zookeeper);

        if (clusterContainers.getOne(Filter.marathon()).isPresent()) {
            Marathon marathon = (Marathon) clusterContainers.getOne(Filter.marathon()).get();
            marathon.setZooKeeper(zookeeper);
        }

        clusterContainers.getContainers().stream().filter(Filter.mesosAgent()).forEach(a -> {
            MesosAgent agent = (MesosAgent) a;
            agent.setZooKeeper(zookeeper);
        });

        if (clusterContainers.getOne(Filter.registrator()).isPresent()) {
            Consul consul = (Consul) clusterContainers.getOne(Filter.consul()).get();
            Registrator registrator = (Registrator) clusterContainers.getOne(Filter.registrator()).get();
            registrator.setConsul(consul);
        }
    }

    private static Boolean isPresent(ClusterContainers clusterContainers, Predicate<ClusterProcess> filter) {
        return clusterContainers.isPresent(filter);
    }

}
