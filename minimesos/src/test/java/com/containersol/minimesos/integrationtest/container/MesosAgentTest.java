package com.containersol.minimesos.integrationtest.container;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.mesos.MesosAgentContainer;
import org.junit.Assert;
import org.junit.Test;

public class MesosAgentTest {

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
            Assert.fail("Pulling non-existing image should result in an exception");
        } catch (MinimesosException mme) {
            Assert.assertTrue("Name of the image should be in the error message: " + mme.getMessage(), mme.getMessage().contains(imageTag));
        }
    }

}
