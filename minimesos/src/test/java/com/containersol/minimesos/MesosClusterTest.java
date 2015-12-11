package com.containersol.minimesos;

import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.*;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Link;
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

import static org.junit.Assert.*;

public class MesosClusterTest {

    protected static final String resources = MesosSlave.DEFAULT_PORT_RESOURCES + "; cpus(*):0.2; mem(*):256; disk(*):200";
    protected static final DockerClient dockerClient = DockerClientFactory.build();
    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder(dockerClient)
            .withZooKeeper()
            .withMaster()
            .withSlave(zooKeeper -> new MesosSlaveExtended(dockerClient, resources, "5051", zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, MesosContainer.MESOS_IMAGE_TAG))
            .withSlave(zooKeeper -> new MesosSlaveExtended(dockerClient, resources, "5051", zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, MesosContainer.MESOS_IMAGE_TAG))
            .withSlave(zooKeeper -> new MesosSlaveExtended(dockerClient, resources, "5051", zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, MesosContainer.MESOS_IMAGE_TAG))
            .withMarathon(zooKeeper -> new Marathon(dockerClient, zooKeeper, true ))
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @After
    public void after() {
        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);
        util.getContainers(true).filterByName("^mesos-[0-9a-f\\-]*S\\d*\\.[0-9a-f\\-]*$").remove();
    }

    @Test
    public void mesosClusterStateInfoJSONMatchesSchema() throws UnirestException, JsonParseException, JsonMappingException {
        assertNotNull( CLUSTER.getClusterStateInfo() );
    }

    @Test
    public void mesosAgentStateInfoJSONMatchesSchema() throws UnirestException, JsonParseException, JsonMappingException {
        String slaveId = CLUSTER.getSlaves()[0].getContainerId();
        String state = MesosCluster.getContainerStateInfo(slaveId);
        assertNotNull( state );
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        JSONObject stateInfo = CLUSTER.getStateInfoJSON();

        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
    }

    @Test
    public void mesosResourcesCorrect() throws Exception {
        JSONObject stateInfo = CLUSTER.getStateInfoJSON();
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals((long) 0.2, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getLong("cpus"));
            Assert.assertEquals(256, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getInt("mem"));
        }
    }

    @Test
    public void dockerExposeResourcesPorts() throws Exception {

        DockerClient docker = CONFIG.dockerClient;
        List<MesosSlave> containers = Arrays.asList(CLUSTER.getSlaves());

        for (MesosSlave container : containers) {
            ArrayList<Integer> ports = MesosSlave.parsePortsFromResource(((MesosSlaveExtended)container).getResources());
            InspectContainerResponse response = docker.inspectContainerCmd(container.getContainerId()).exec();
            Map bindings = response.getNetworkSettings().getPorts().getBindings();
            for (Integer port : ports) {
                Assert.assertTrue(bindings.containsKey(new ExposedPort(port)));
            }
        }

    }

    @Test
    public void testPullAndStartContainer() throws UnirestException {
        HelloWorldContainer container = new HelloWorldContainer(CONFIG.dockerClient);
        String containerId = CLUSTER.addAndStartContainer(container, MesosContainer.DEFAULT_TIMEOUT_SEC);
        String ipAddress = DockerContainersUtil.getIpAddress(CONFIG.dockerClient, containerId);
        String url = "http://" + ipAddress + ":80";
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    @Test
    public void testMasterLinkedToSlaves() throws UnirestException {
        List<MesosSlave> containers = Arrays.asList(CLUSTER.getSlaves());
        for (MesosSlave container : containers) {
            InspectContainerResponse exec = CONFIG.dockerClient.inspectContainerCmd(container.getContainerId()).exec();

            List<Link> links = Arrays.asList(exec.getHostConfig().getLinks());

            assertNotNull( links );
            assertEquals( "link to zookeeper is expected", 1, links.size() );
            assertEquals( "minimesos-zookeeper", links.get(0).getAlias() );

        }
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
                CONFIG.dockerClient,
                "ports(*):[9204-9204, 9304-9304]; cpus(*):0.2; mem(*):256; disk(*):200",
                "5051",
                CLUSTER.getZkContainer(),
                "containersol/mesos-agent",
                MesosContainer.MESOS_IMAGE_TAG) {

            @Override
            protected CreateContainerCmd dockerCommand() {
                CreateContainerCmd containerCmd = super.dockerCommand();
                containerCmd.withEntrypoint(
                        "mesos-execute",
                        "--master=" + CLUSTER.getMesosMasterContainer().getIpAddress() + ":5050",
                        "--docker_image=busybox",
                        "--command=echo 1",
                        "--name=test-cmd",
                        "--resources=cpus(*):0.1;mem(*):256"
                );
                return containerCmd;
            }
        };

        CLUSTER.addAndStartContainer(mesosSlave, MesosContainer.DEFAULT_TIMEOUT_SEC);
        LogContainerTestCallback cb = new LogContainerTestCallback();
        CONFIG.dockerClient.logContainerCmd(mesosSlave.getContainerId()).withStdOut().exec(cb);
        cb.awaitCompletion();

        Awaitility.await().atMost(Duration.ONE_MINUTE).until(() -> {
            LogContainerTestCallback cb1 = new LogContainerTestCallback();
            CONFIG.dockerClient.logContainerCmd(mesosSlave.getContainerId()).withStdOut().exec(cb1);
            cb1.awaitCompletion();
            String log = cb1.toString();
            return log.contains("Received status update TASK_FINISHED for task test-cmd");
        });

    }

}
