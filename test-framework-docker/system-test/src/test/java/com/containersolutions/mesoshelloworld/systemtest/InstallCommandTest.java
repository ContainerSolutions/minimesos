package com.containersolutions.mesoshelloworld.systemtest;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersolutions.mesoshelloworld.executor.Executor;
import com.containersolutions.mesoshelloworld.scheduler.Configuration;
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

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.fail;

public class InstallCommandTest {

    public static final String MESOS_MASTER_IP_TOKEN = "${MESOS_MASTER_IP}";

    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder()
            .withZooKeeper()
            .withMaster()
            .withAgent("ports(*):[8081-8082]")
            .withMarathon(Marathon::new)
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @Test
    public void testMesosInstall() throws IOException {
        deployApp();

        DockerContainersUtil util = new DockerContainersUtil();
        final List<String> ipAddresses = new ArrayList<>();
        await("executors are expected to come up").atMost(60, TimeUnit.SECONDS).until(() -> {
            ipAddresses.clear();
            ipAddresses.addAll(util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).getIpAddresses());
            return ipAddresses.size() > 0;
        });

        String appUrl1 = "http://" + ipAddresses.get(0) + ":8081";
        String appUrl2 = "http://" + ipAddresses.get(0) + ":8082";
        await("The app did not start providing expected output").atMost(60, TimeUnit.SECONDS).until( () -> {
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

            return response1.equals(Executor.RESPONSE_STRING) || response2.equals(Executor.RESPONSE_STRING);
        });
    }

    private void deployApp() throws IOException {
        File taskFile = new File("src/test/resources/test-framework-docker.json");
        if (!taskFile.exists()) {
            fail("Failed to find task info file " + taskFile.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(taskFile)) {
            String appJson = IOUtils.toString(fis);
            appJson = appJson.replace(MESOS_MASTER_IP_TOKEN, CLUSTER.getMasterContainer().getIpAddress());
            CLUSTER.getMarathonContainer().deployApp(appJson);
        }
    }

    @AfterClass
    public static void removeExecutors() {
        DockerContainersUtil util = new DockerContainersUtil();

        // stop container, otherwise it keeps on scheduling new executors as soon as they are stopped
        util.getContainers(false).filterByImage(SchedulerContainer.SCHEDULER_IMAGE).kill().remove();
        // remove executors
        util.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).kill().remove();
    }

}
