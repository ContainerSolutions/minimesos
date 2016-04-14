package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersol.minimesos.mesos.MesosAgentContainer;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DynamicClusterTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = new MesosClusterTestRule(new File("src/test/resources/configFiles/minimesosFile-dynamicClusterTest"));

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void noMarathonTest() throws FileNotFoundException {
        String clusterId = CLUSTER.getClusterId();

        assertNotNull("Cluster ID must be set", clusterId);

        // this should not throw any exceptions
        CLUSTER.stop(RULE.getFactory());
    }

    @Test
    public void stopWithNewContainerTest() {
        ZooKeeper zooKeeper = CLUSTER.getZooKeeper();
        MesosAgent extraAgent = new MesosAgentContainer(zooKeeper);

        String containerId = CLUSTER.addAndStartProcess(extraAgent);
        assertNotNull("freshly started container is not found", DockerContainersUtil.getContainer(containerId));

        CLUSTER.stop(RULE.getFactory());
        assertNull("new container should be stopped too", DockerContainersUtil.getContainer(containerId));
    }

}
