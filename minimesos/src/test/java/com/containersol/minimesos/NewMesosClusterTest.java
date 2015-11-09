package com.containersol.minimesos;

import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Replicates MesosClusterTest with new API
 */
public class NewMesosClusterTest {

    private DockerClient dockerClient = DockerClientFactory.build();

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(new ClusterArchitecture.Builder().build());

    @After
    public void after() {
        if( cluster != null ) {
            DockerContainersUtil util = new DockerContainersUtil(dockerClient);
            util.getContainers(true).filterByName("^mesos-[0-9a-f\\-]*S\\d*\\.[0-9a-f\\-]*$").remove();
        }
    }

    @Test
    public void mesosClusterStateInfoJSONMatchesSchema() throws UnirestException, JsonParseException, JsonMappingException {
        cluster.getStateInfo();
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        JSONObject stateInfo = cluster.getStateInfoJSON();

        Assert.assertEquals(1, stateInfo.getInt("activated_slaves")); // Only one slave is actually _required_ to have a cluster
    }

    @Test
    public void mesosResourcesCorrect() throws Exception {
        JSONObject stateInfo = cluster.getStateInfoJSON();
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals((long) 0.2, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getLong("cpus"));
            Assert.assertEquals(256, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getInt("mem"));
        }
    }

    @Test
    public void dockerExposeResourcesPorts() throws Exception {
        String resources = MesosSlave.DEFAULT_PORT_RESOURCES;
        ArrayList<Integer> ports = MesosSlave.parsePortsFromResource(resources);
        List<MesosSlave> containers = Arrays.asList(cluster.getSlaves());
        for (MesosSlave container : containers) {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
            Map bindings = response.getNetworkSettings().getPorts().getBindings();
            for (Integer port : ports) {
                Assert.assertTrue(bindings.containsKey(new ExposedPort(port)));
            }
        }
    }

    @Test
    public void testPullAndStartContainer() throws UnirestException {
        HelloWorldContainer container = new HelloWorldContainer(dockerClient);
        String containerId = cluster.addAndStartContainer(container);
        String ipAddress = dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
        String url = "http://" + ipAddress + ":80";
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    public static class LogContainerTestCallback extends LogContainerResultCallback {
        protected final StringBuffer log = new StringBuffer();

        @Override
        public void onNext(Frame frame) {
            log.append(new String(frame.getPayload()));
            super.onNext(frame);
        }

        @Override
        public String toString() {
            return log.toString();
        }
    }

    @Test
    public void testMesosExecuteContainerSuccess() throws InterruptedException {
        MesosSlaveExtended mesosSlave = new MesosSlaveExtended(
                dockerClient,
                "ports(*):[9204-9204, 9304-9304]; cpus(*):0.2; mem(*):256; disk(*):200",
                "5051",
                cluster.getZkContainer(),
                "containersol/mesos-agent",
                "0.25.0-0.2.70.ubuntu1404", MesosCluster.getClusterId()) {

            @Override
            protected CreateContainerCmd dockerCommand() {
                CreateContainerCmd containerCmd = super.dockerCommand();
                containerCmd.withEntrypoint(
                        "mesos-execute",
                        "--master=" + cluster.getMesosMasterContainer().getIpAddress() + ":5050",
                        "--docker_image=busybox",
                        "--command=echo 1",
                        "--name=test-cmd",
                        "--resources=cpus(*):0.1;mem(*):256"
                );
                return containerCmd;
            }
        };

        cluster.addAndStartContainer(mesosSlave);
        LogContainerTestCallback cb = new LogContainerTestCallback();
        dockerClient.logContainerCmd(mesosSlave.getContainerId()).withStdOut().exec(cb);
        cb.awaitCompletion();

        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> {
            LogContainerTestCallback cb1 = new LogContainerTestCallback();
            dockerClient.logContainerCmd(mesosSlave.getContainerId()).withStdOut().exec(cb1);
            cb1.awaitCompletion();
            String log = cb1.toString();
            return log.contains("Received status update TASK_FINISHED for task test-cmd");
        });

    }

}
