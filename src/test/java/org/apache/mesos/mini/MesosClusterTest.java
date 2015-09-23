package org.apache.mesos.mini;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosSlave;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        ArrayList<Integer> ports = new ArrayList();
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
}
