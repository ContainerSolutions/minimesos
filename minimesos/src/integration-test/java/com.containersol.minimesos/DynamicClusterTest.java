package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersol.minimesos.mesos.MesosAgentContainer;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.FileNotFoundException;

public class DynamicClusterTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-dynamicClusterTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void noMarathonTest() throws FileNotFoundException {
        String clusterId = CLUSTER.getClusterId();

        Assert.assertNotNull("Cluster ID must be set", clusterId);

        // this should not throw any exceptions
        CLUSTER.destroy(RULE.getFactory());
    }

    @Test
    public void stopWithNewContainerTest() {
        MesosAgent extraAgent = new MesosAgentContainer(new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION));
        ZooKeeper zooKeeper = CLUSTER.getZooKeeper();
        extraAgent.setZooKeeper(zooKeeper);

        String containerId = CLUSTER.addAndStartProcess(extraAgent);
        Assert.assertNotNull("freshly started container is not found", DockerContainersUtil.getContainer(containerId));

        CLUSTER.destroy(RULE.getFactory());
        Assert.assertNull("new container should be stopped too", DockerContainersUtil.getContainer(containerId));
    }

}
