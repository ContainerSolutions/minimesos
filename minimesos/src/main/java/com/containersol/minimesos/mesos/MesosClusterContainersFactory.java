package com.containersol.minimesos.mesos;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.Consul;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.cluster.MesosMaster;
import com.containersol.minimesos.cluster.Registrator;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.container.ContainerName;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.MarathonContainer;
import com.github.dockerjava.api.model.Container;

import com.github.dockerjava.api.model.ContainerPort;
import org.apache.commons.io.IOUtils;

/**
 * Docker based factory of minimesos cluster members
 */
public class MesosClusterContainersFactory extends MesosClusterFactory {

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

    @Override
    public void loadRunningCluster(MesosCluster cluster) {
        String clusterId = cluster.getClusterId();
        List<ClusterProcess> containers = cluster.getMemberProcesses();

        List<Container> dockerContainers = DockerContainersUtil.getContainers(false).getContainers();
        Collections.sort(dockerContainers, (c1, c2) -> Long.compare(c1.getCreated(), c2.getCreated()));

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
                            restoreMesosVersion(cluster, master);
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
    }

    private void restoreMesosVersion(MesosCluster cluster, MesosMaster master) {
        String mesosVersion = master.getState().getVersion();
        String[] versionComponents = mesosVersion.split("\\.");
        cluster.setMesosVersion(versionComponents[0] + "." + versionComponents[1]);
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

    public MesosCluster createMesosCluster(InputStream inputStream) {
        try {
            ClusterConfig clusterConfig = new ConfigParser().parse(IOUtils.toString(inputStream));
            return createMesosCluster(clusterConfig);
        } catch (IOException e) {
            throw new MinimesosException("Could not read minimesos config:" + e.getCause());
        }
    }

    public MesosCluster createMesosCluster(ClusterConfig clusterConfig) {
        ClusterContainers clusterContainers = ClusterArchitecture.Builder.createCluster(clusterConfig).build().getClusterContainers();
        List<ClusterProcess> processes = clusterContainers.getContainers();
        return new MesosCluster(clusterConfig, processes);
    }

}
