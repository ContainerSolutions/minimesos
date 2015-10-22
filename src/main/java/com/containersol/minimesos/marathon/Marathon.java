package com.containersol.minimesos.marathon;

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

    private static Logger LOGGER = Logger.getLogger(Marathon.class);

    private static final String MARATHON_IMAGE = "mesosphere/marathon";
    public static final String REGISTRY_TAG = "v0.8.1";

    private String clusterId;

    private ZooKeeper zooKeeper;

    private Boolean exposedHostPort;

    public Marathon(DockerClient dockerClient, String clusterId, ZooKeeper zooKeeper, Boolean exposedHostPort) {
        super(dockerClient);
        this.clusterId = clusterId;
        this.zooKeeper = zooKeeper;
        this.exposedHostPort = exposedHostPort;
    }

    @Override
    protected void pullImage() {
        pullImage(MARATHON_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort tcp8080 = ExposedPort.tcp(8080);
        Ports portBindings = new Ports();
        if (exposedHostPort) {
            portBindings.bind(tcp8080, Ports.Binding(8080));
        }
        return dockerClient.createContainerCmd(MARATHON_IMAGE + ":" + REGISTRY_TAG)
                .withName("minimesos-marathon-" + clusterId + "-" + getRandomId())
                .withExtraHosts("minimesos-zookeeper:" + this.zooKeeper.getIpAddress())
                .withCmd("--master", "zk://minimesos-zookeeper:2181/mesos", "--zk", "zk://minimesos-zookeeper:2181/marathon")
                .withExposedPorts(tcp8080)
                .withPortBindings(portBindings);
    }

}
