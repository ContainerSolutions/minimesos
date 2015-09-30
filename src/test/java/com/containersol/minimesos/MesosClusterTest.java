package com.containersol.minimesos;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.containersol.minimesos.mesos.MesosClusterConfig;
import com.containersol.minimesos.mesos.MesosSlave;
import org.apache.commons.lang.ArrayUtils;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosSlave;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

public class MesosClusterTest {

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(
        MesosClusterConfig.builder()
            .numberOfSlaves(3)
            .slaveResources(new String[]{
                    "ports(*):[9201-9201, 9301-9301]; cpus(*):0.2; mem(*):256; disk(*):200",
                    "ports(*):[9202-9202, 9302-9302]; cpus(*):0.2; mem(*):256; disk(*):200",
                    "ports(*):[9203-9203, 9303-9303]; cpus(*):0.2; mem(*):256; disk(*):200"
            })
            .build()
    );

    @Test
    public void mesosClusterStateInfoJSONMatchesSchema() throws UnirestException, JsonParseException, JsonMappingException {
        cluster.getStateInfo();
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        JSONObject stateInfo = cluster.getStateInfoJSON();

        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
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
    public void dockerExposeResourcesPorts() {
        DockerClient docker = cluster.getMesosMasterContainer().getOuterDockerClient();
        List<MesosSlave> containers = Arrays.asList(cluster.getSlaves());
        ArrayList<Integer> ports = new ArrayList<>();
        for (MesosSlave container : containers) {
            try {
                ports = container.parsePortsFromResource(container.getResources());
            } catch (Exception e) {
                e.printStackTrace();
            }
            InspectContainerResponse response = docker.inspectContainerCmd(container.getContainerId()).exec();
            Map bindings = response.getNetworkSettings().getPorts().getBindings();
            for (Integer port : ports) {
                Assert.assertTrue(bindings.containsKey(new ExposedPort(port)));
            }
        }
    }

    @Test
    public void testPullAndStartContainer() throws UnirestException {
        HelloWorldContainer container = new HelloWorldContainer(cluster.getConfig().dockerClient);
        String containerId = cluster.addAndStartContainer(container);
        String ipAddress = cluster.getConfig().dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
        String url = "http://" + ipAddress + ":80";
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    @Test
    public void testMasterLinkedToSlaves() throws UnirestException {
        List<MesosSlave> containers = Arrays.asList(cluster.getSlaves());
        for (MesosSlave container : containers) {
            InspectContainerResponse exec = cluster.getMesosMasterContainer().getOuterDockerClient().inspectContainerCmd(container.getContainerId()).exec();
            List<Link> links = Arrays.asList(exec.getHostConfig().getLinks());
            for (Link link : links) {
                Assert.assertEquals("mini-mesos-master", link.getAlias());
            }
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
        CreateContainerCmd cmd = new MesosSlave(
                cluster.getMesosMasterContainer().getOuterDockerClient(),
                "cpus(*):0.2; mem(*):256; disk(*):200",
                "5051",
                cluster.getZkUrl(),
                cluster.getMesosMasterContainer().getContainerId()).getBaseCommand()
                .withEntrypoint(
                        "mesos-execute",
                        "--master=" + cluster.getMesosMasterContainer().getIpAddress() + ":5050",
                        "--docker_image=busybox",
                        "--command=echo 1",
                        "--name=test-cmd",
                        "--resources=cpus(*):0.1;mem(*):256"
                );

        String containerId = cmd.exec().getId();
        cluster.getMesosMasterContainer().getOuterDockerClient().startContainerCmd(containerId).exec();
        LogContainerTestCallback cb = new LogContainerTestCallback();
        cluster.getMesosMasterContainer().getOuterDockerClient().logContainerCmd(containerId).withStdOut().exec(cb);
        cb.awaitCompletion();
        Assert.assertThat(cb.toString().contains("task test-cmd submitted to slave"), is(equalTo(true)));

        Awaitility.await().atMost(Duration.FIVE_SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                LogContainerTestCallback cb = new LogContainerTestCallback();
                cluster.getMesosMasterContainer().getOuterDockerClient().logContainerCmd(containerId).withStdOut().exec(cb);
                cb.awaitCompletion();
                return cb.toString().contains("Received status update TASK_FINISHED for task test-cmd");
            }
        });


    }
}
