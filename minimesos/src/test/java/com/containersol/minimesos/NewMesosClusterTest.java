package com.containersol.minimesos;

import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;

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

import static org.junit.Assert.assertEquals;

/**
 * Replicates MesosClusterTest with new API
 */

public class NewMesosClusterTest {

    private DockerClient dockerClient = DockerClientFactory.build();

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(new ClusterArchitecture.Builder().build());

    @After
    public void after() {
        DockerContainersUtil util = new DockerContainersUtil(dockerClient);
        util.getContainers(false).filterByName("^mesos-[0-9a-f\\-]*S\\d*\\.[0-9a-f\\-]*$").remove();
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        JSONObject stateInfo = cluster.getMesosMasterContainer().getStateInfoJSON();

        assertEquals(1, stateInfo.getInt("activated_slaves")); // Only one slave is actually _required_ to have a cluster
    }

    @Test
    public void mesosResourcesCorrect() throws Exception {
        JSONObject stateInfo = cluster.getMesosMasterContainer().getStateInfoJSON();
        for (int i = 0; i < 3; i++) {
            assertEquals((long) 0.2, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getLong("cpus"));
            assertEquals(256, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getInt("mem"));
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
        String containerId = cluster.addAndStartContainer(container, MesosContainer.DEFAULT_TIMEOUT_SEC);
        String ipAddress = DockerContainersUtil.getIpAddress(dockerClient, containerId);
        String url = "http://" + ipAddress + ":80";
        assertEquals(200, Unirest.get(url).asString().getStatus());
    }
}