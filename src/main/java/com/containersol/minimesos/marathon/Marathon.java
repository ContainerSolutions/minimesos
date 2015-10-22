package com.containersol.minimesos.marathon;

import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.apache.log4j.Logger;

/**
 * Marathon container
 */
public class Marathon extends AbstractContainer {

    private static Logger LOGGER = Logger.getLogger(Marathon.class);

    private static final String MARATHON_IMAGE = "mesosphere/marathon";
    public static final String REGISTRY_TAG = "v0.11.1";

    private String clusterId;

    private ZooKeeper zooKeeper;

    public Marathon(DockerClient dockerClient, String clusterId, ZooKeeper zooKeeper) {
        super(dockerClient);
        this.clusterId = clusterId;
        this.zooKeeper = zooKeeper;
    }

    @Override
    protected void pullImage() {
        pullImage(MARATHON_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(MARATHON_IMAGE + ":" + REGISTRY_TAG)
                .withName("minimesos-marathon-" + clusterId + "-" + getRandomId())
                .withExtraHosts("minimesos-zookeeper:" + this.zooKeeper.getIpAddress())
                .withCmd("--master", "zk://minimesos-zookeeper:2181/mesos", "--zk", "zk://minimesos-zookeeper:2181/marathon");
    }

}
