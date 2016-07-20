package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MesosAgentTest {

    private static final ZooKeeper zooKeeper = new ZooKeeperContainer();

    /**
     * It must be possible to detect wrong image within 30 seconds
     */
    @Test(expected = MinimesosException.class, timeout = 60 * 1000)
    public void testPullingWrongContainer() {
        MesosAgentConfig config = new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION);
        config.setImageTag("non-existing-one");

        MesosAgentContainer agent = new MesosAgentContainer(config);
        agent.pullImage();
    }

    /**
     * Test error message
     */
    @Test
    public void testPullingWrongContainerMessage() {

        String imageTag = "non-existing-one";

        MesosAgentConfig config = new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION);
        config.setImageTag(imageTag);

        MesosAgentContainer agent = new MesosAgentContainer(config);
        try {
            agent.pullImage();
            fail("Pulling non-existing image should result in an exception");
        } catch (MinimesosException mme) {
            assertTrue("Name of the image should be in the error message: " + mme.getMessage(), mme.getMessage().contains(imageTag));
        }
    }

}
