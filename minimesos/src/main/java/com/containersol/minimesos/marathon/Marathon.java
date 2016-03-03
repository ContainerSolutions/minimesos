package com.containersol.minimesos.marathon;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MarathonConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.log4j.Logger;

/**
 * Marathon container
 */
public class Marathon extends AbstractContainer {

    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    private final MarathonConfig config;
    private ZooKeeper zooKeeper;

    public Marathon(DockerClient dockerClient, ZooKeeper zooKeeper) {
        this(dockerClient, zooKeeper, new MarathonConfig());
    }

    public Marathon(DockerClient dockerClient, ZooKeeper zooKeeper, MarathonConfig config) {
        super(dockerClient);
        this.zooKeeper = zooKeeper;
        this.config = config;
    }

    public Marathon(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        super(dockerClient, cluster, uuid, containerId);
        this.config = new MarathonConfig();
    }

    @Override
    public String getRole() {
        return "marathon";
    }

    @Override
    protected void pullImage() {
        pullImage(config.getImageName(), config.getImageTag());
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(MarathonConfig.MARATHON_PORT);
        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(exposedPort, Ports.Binding(MarathonConfig.MARATHON_PORT));
        }
        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName( getName() )
                .withExtraHosts("minimesos-zookeeper:" + this.zooKeeper.getIpAddress())
                .withCmd("--master", "zk://minimesos-zookeeper:2181/mesos", "--zk", "zk://minimesos-zookeeper:2181/marathon")
                .withExposedPorts(exposedPort)
                .withPortBindings(portBindings);
    }

    public void deployApp(String appJson) {
        MarathonClient marathonClient = new MarathonClient(getIpAddress());
        LOGGER.info(String.format("Installing an app on marathon %s", getIpAddress()));
        marathonClient.deployApp(appJson);
    }

}
