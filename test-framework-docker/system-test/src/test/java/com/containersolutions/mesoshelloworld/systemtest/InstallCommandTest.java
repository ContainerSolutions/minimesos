package com.containersolutions.mesoshelloworld.systemtest;

import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.marathon.MarathonClient;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersolutions.mesoshelloworld.executor.Executor;
import com.containersolutions.mesoshelloworld.scheduler.Configuration;
import com.github.dockerjava.api.DockerClient;
import com.jayway.awaitility.Awaitility;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class InstallCommandTest {

    public static final String MESOS_MASTER_IP_TOKEN = "${MESOS_MASTER_IP}";

    protected static final DockerClient dockerClient = DockerClientFactory.build();
    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder(dockerClient)
            .withZooKeeper()
            .withMaster()
            .withSlave("ports(*):[8081-8082]")
            .withMarathon(zooKeeper -> new Marathon(dockerClient, zooKeeper, true ))
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @Test
    public void testMesosInstall() throws IOException {

        File taskFile = new File( "src/test/resources/test-framework-docker.json" );
        if (!taskFile.exists()) {
            fail("Failed to find task info file " + taskFile.getAbsolutePath());
        }

        // wait for Marathon to start
        MarathonClient marathon = new MarathonClient( CLUSTER.getMarathonContainer().getIpAddress() );
        Awaitility.await("Marathon did not start responding").atMost(60, TimeUnit.SECONDS).until(marathon::isReady);

        String masterIp = CLUSTER.getMesosMasterContainer().getIpAddress();

        try (FileInputStream fis = new FileInputStream(taskFile)) {
            String taskJson = IOUtils.toString(fis);
            taskJson = taskJson.replace( MESOS_MASTER_IP_TOKEN, masterIp );
            CLUSTER.executeMarathonTask(taskJson);
        }

        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);
        final List<String> ipAddresses = new ArrayList<>();
        Awaitility.await("executors are expected to come up").atMost(60, TimeUnit.SECONDS).until(() -> {
            ipAddresses.clear();
            ipAddresses.addAll(util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).getIpAddresses());
            return ipAddresses.size() > 0;
        });

        String appUrl1 = "http://" + ipAddresses.get(0) + ":8081";
        String appUrl2 = "http://" + ipAddresses.get(0) + ":8082";
        Awaitility.await("The app did not start providing expected output").atMost(60, TimeUnit.SECONDS).until( () -> {

            String response1;
            try {
                response1 = Unirest.get(appUrl1).asString().getBody();
            } catch (UnirestException e) {
                response1 = "";
            }
            String response2;
            try {
                response2 = Unirest.get(appUrl2).asString().getBody();
            } catch (UnirestException e) {
                response2 = "";
            }

            return response1.equals( Executor.RESPONSE_STRING ) || response2.equals( Executor.RESPONSE_STRING );

        });

    }

    @AfterClass
    public static void removeExecutors() {

        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);

        // stop container, otherwise it keeps on scheduling new executors as soon as they are stopped
        util.getContainers(false).filterByImage(SchedulerContainer.SCHEDULER_IMAGE).kill().remove();
        // remove executors
        util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).kill().remove();

    }

}
