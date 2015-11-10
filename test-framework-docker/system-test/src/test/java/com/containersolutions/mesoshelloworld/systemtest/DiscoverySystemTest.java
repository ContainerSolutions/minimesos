package com.containersolutions.mesoshelloworld.systemtest;

import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersolutions.mesoshelloworld.scheduler.Configuration;
import org.apache.log4j.Logger;
import com.containersol.minimesos.MesosCluster;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests REST node discovery
 */
public class DiscoverySystemTest {

    public static final Logger LOGGER = Logger.getLogger(DiscoverySystemTest.class);

    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder()
            .withZooKeeper()
            .withMaster()
            // TODO: configuration of the first slave should result in the test failure, but it does not
            .withSlave("ports(*):[8081-8083]")
            .withSlave("ports(*):[8080-8082]")
            .withSlave("ports(*):[8080-8082]")
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @BeforeClass
    public static void startScheduler() throws Exception {

        LOGGER.info("Starting Scheduler");
        String ipAddress = CLUSTER.getMesosMasterContainer().getIpAddress();
        SchedulerContainer scheduler = new SchedulerContainer(CONFIG.dockerClient, ipAddress);

        // Cluster now has responsibility to shut down container
        CLUSTER.addAndStartContainer(scheduler);

        LOGGER.info("Started Scheduler on " + scheduler.getIpAddress());
    }

    @Test
    public void testNodeDiscoveryRest() {

        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);
        Set<String> ipAddresses = util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).getIpAddresses();
        // assertEquals( "Problem with running tasks count", 9, ipAddresses.size());

        long timeout = 120;
        HelloWorldResponse helloWorldResponse = new HelloWorldResponse( ipAddresses, Arrays.asList(8080, 8081, 8082), timeout );
        assertTrue("Elasticsearch nodes did not discover each other within " + timeout + " seconds", helloWorldResponse.isDiscoverySuccessful());
    }

    @AfterClass
    public static void removeExecutors() {
        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);
        util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).kill().remove();
    }

}
