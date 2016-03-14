package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.github.dockerjava.api.DockerClient;
import org.junit.Test;

public class MesosAgentTest {

    private static final DockerClient dockerClient = DockerClientFactory.build();
    private static final ZooKeeper zooKeeper = new ZooKeeper(dockerClient);

    @Test(expected = MinimesosException.class, timeout = 30 * 1000)
    /**
     * It must be possible to detect wrong image within 30 seconds
     */
    public void testPullingWrongContainer() {

        MesosAgentConfig config = new MesosAgentConfig();
        config.setImageTag("non-existing-one");

        MesosAgent agent = new MesosAgent(dockerClient, zooKeeper, config);
        agent.pullImage();

    }

}
