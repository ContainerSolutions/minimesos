package com.containersolutions.mesoshelloworld.systemtest;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersolutions.mesoshelloworld.executor.FrameworkExecutor;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.fail;

public class InstallCommandTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-install-command-test");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void testMesosInstall() throws IOException {
        deployApp();

        List<String> ipAddresses = new ArrayList<>();
        await("executors are expected to come up").atMost(60, TimeUnit.SECONDS).until(() -> {
            Set<String> runningNow = DockerContainersUtil.getContainers(false).filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).getIpAddresses();
            if (runningNow.size() > 0) {
                ipAddresses.addAll(runningNow);
            }
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

            return response1.equals(FrameworkExecutor.RESPONSE_STRING) || response2.equals(FrameworkExecutor.RESPONSE_STRING);
        });
    }

    private void deployApp() throws IOException {
        File taskFile = new File("src/test/resources/test-framework-docker.json");
        if (!taskFile.exists()) {
            fail("Failed to find task info file " + taskFile.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(taskFile)) {
            String appJson = IOUtils.toString(fis);
            CLUSTER.getMarathon().deployApp(appJson);
        }
    }

    @AfterClass
    public static void removeExecutors() {
        DockerContainersUtil containers = DockerContainersUtil.getContainers(true);

        // stop container, otherwise it keeps on scheduling new executors as soon as they are stopped
        containers.filterByImage(SchedulerContainer.SCHEDULER_IMAGE).kill(true).remove();
        // remove executors
        containers.filterByImage(Configuration.DEFAULT_EXECUTOR_IMAGE).kill(true).remove();
    }

}
