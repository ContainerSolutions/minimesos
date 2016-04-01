package com.containersol.minimesos.main.factory;

import com.containersol.minimesos.cluster.*;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.container.ContainerName;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.marathon.MarathonContainer;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.model.Container;

import java.util.Collections;
import java.util.List;

/**
 * Docker based factory of minimesos cluster members
 */
public class MesosClusterContainersFactory extends MesosClusterFactory {

    @Override
    public ZooKeeper createZooKeeper(MesosCluster mesosCluster, String uuid, String containerId) {
        return new ZooKeeperContainer(mesosCluster, uuid, containerId);
    }

    @Override
    public MesosAgent createMesosAgent(MesosCluster mesosCluster, String uuid, String containerId) {
        return new MesosAgentContainer(mesosCluster, uuid, containerId);
    }

    @Override
    public MesosMaster createMesosMaster(MesosCluster mesosCluster, String uuid, String containerId) {
        return new MesosMasterContainer(mesosCluster, uuid, containerId);
    }

    @Override
    public Marathon createMarathon(MesosCluster mesosCluster, String uuid, String containerId) {
        return new MarathonContainer(mesosCluster, uuid, containerId);
    }

    @Override
    public Consul createConsul(MesosCluster mesosCluster, String uuid, String containerId) {
        return new ConsulContainer(mesosCluster, uuid, containerId);
    }

    @Override
    public Registrator createRegistrator(MesosCluster mesosCluster, String uuid, String containerId) {
        return new RegistratorContainer(mesosCluster, uuid, containerId);
    }

    @Override
    public void loadRunningCluster(MesosCluster cluster) {

        String clusterId = cluster.getClusterId();
        List<AbstractContainer> containers = cluster.getContainers();

        List<Container> dockerContainers = DockerClientFactory.build().listContainersCmd().exec();
        Collections.sort(dockerContainers, (c1, c2) -> Long.compare(c1.getCreated(), c2.getCreated()));

        for (Container container : dockerContainers) {
            String name = ContainerName.getFromDockerNames(container.getNames());
            if (ContainerName.belongsToCluster(name, clusterId)) {

                String containerId = container.getId();
                String[] parts = name.split("-");
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
                        // restore "exposed ports" attribute
                        Container.Port[] ports = container.getPorts();
                        if (ports != null) {
                            for (Container.Port port : ports) {
                                if (port.getIp() != null && port.getPrivatePort() == MesosMasterConfig.MESOS_MASTER_PORT) {
                                    cluster.setExposedHostPorts(true);
                                }
                            }
                        }
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
                }

            }

        }
    }

    @Override
    public void destroyRunningCluster(String clusterId) {
        List<Container> containers = DockerClientFactory.build().listContainersCmd().exec();
        for (Container container : containers) {
            if (ContainerName.belongsToCluster(container.getNames(), clusterId)) {
                DockerClientFactory.build().removeContainerCmd(container.getId()).withForce().withRemoveVolumes(true).exec();
            }
        }
    }
}
