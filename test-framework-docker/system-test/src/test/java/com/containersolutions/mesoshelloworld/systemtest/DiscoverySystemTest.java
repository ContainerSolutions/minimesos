package com.containersolutions.mesoshelloworld.systemtest;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersolutions.mesoshelloworld.scheduler.Configuration;
import com.jayway.awaitility.Awaitility;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Tests REST node discovery
 */
public class DiscoverySystemTest {

    public static final Logger LOGGER = Logger.getLogger(DiscoverySystemTest.class);

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-discoverySystemTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    public static final long TIMEOUT = 120;

    @BeforeClass
    public static void startScheduler() throws Exception {
        String ipAddress = CLUSTER.getMaster().getIpAddress();

        LOGGER.info("Starting Scheduler, connected to " + ipAddress);
        SchedulerContainer scheduler = new SchedulerContainer(ipAddress);

        // Cluster now has responsibility to shut down container
        CLUSTER.addAndStartProcess(scheduler);

        LOGGER.info("Started Scheduler on " + scheduler.getIpAddress());
    }

    @Test
    public void testNodeDiscoveryRest() {
        DockerContainersUtil util = new DockerContainersUtil();

        final Set<String> ipAddresses = new HashSet<>();
        Awaitility.await("9 expected executors did not come up").atMost(TIMEOUT, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            ipAddresses.clear();
            ipAddresses.addAll(util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).getIpAddresses());
            LOGGER.info(ipAddresses.size() + " executors running");
            return ipAddresses.size() == 9;
        });

        HelloWorldResponse helloWorldResponse = new HelloWorldResponse(ipAddresses, Arrays.asList(8080, 8081, 8082), TIMEOUT);
        assertTrue("Executors did not come up within " + TIMEOUT + " seconds", helloWorldResponse.isDiscoverySuccessful());
    }

    @AfterClass
    public static void removeExecutors() {
        DockerContainersUtil util = new DockerContainersUtil();

        // stop scheduler, otherwise it keeps on scheduling new executors as soon as they are stopped
        util.getContainers(false).filterByImage(SchedulerContainer.SCHEDULER_IMAGE).kill().remove();

        DockerContainersUtil executors = util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE);
        LOGGER.info(String.format("Found %d containers to stop and remove", executors.size()));
        executors.kill(true).remove();
    }

}
