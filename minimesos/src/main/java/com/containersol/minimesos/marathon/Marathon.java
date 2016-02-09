package com.containersol.minimesos.marathon;

import com.containersol.minimesos.MesosCluster;
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

    private static final String MARATHON_IMAGE = "mesosphere/marathon";
    public static final String MARATHON_IMAGE_TAG = "v0.13.0";
    public static final int MARATHON_PORT = 8080;

    private ZooKeeper zooKeeper;
    private String marathonImageTag = MARATHON_IMAGE_TAG;
    private Boolean exposedHostPort;

    public Marathon(DockerClient dockerClient, ZooKeeper zooKeeper, String marathonImageTag, Boolean exposedHostPort) {
        this( dockerClient, zooKeeper, exposedHostPort);
        this.marathonImageTag = marathonImageTag;
    }

    public Marathon(DockerClient dockerClient, ZooKeeper zooKeeper, Boolean exposedHostPort) {
        super(dockerClient);
        this.zooKeeper = zooKeeper;
        this.exposedHostPort = exposedHostPort;
    }

    public Marathon(DockerClient dockerClient, String clusterId, String uuid, String containerId) {
        super(dockerClient, clusterId, uuid, containerId);
    }

    @Override
    protected String getRole() {
        return "marathon";
    }

    @Override
    protected void pullImage() {
        pullImage(MARATHON_IMAGE, marathonImageTag);
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(MARATHON_PORT);
        Ports portBindings = new Ports();
        if (exposedHostPort) {
            portBindings.bind(exposedPort, Ports.Binding(MARATHON_PORT));
        }
        return dockerClient.createContainerCmd(MARATHON_IMAGE + ":" + marathonImageTag)
                .withName( buildContainerName() )
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
