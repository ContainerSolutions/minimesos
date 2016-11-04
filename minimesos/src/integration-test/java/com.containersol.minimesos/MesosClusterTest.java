package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosMaster;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.util.ResourceUtil;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MesosClusterTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-mesosClusterTest");

    public static final MesosCluster CLUSTER = RULE.getMesosCluster();

    @After
    public void after() {
        DockerContainersUtil.getContainers(false).filterByName(HelloWorldContainer.CONTAINER_NAME_PATTERN).kill().remove();
    }

    @Test(expected = MinimesosException.class)
    public void testLoadCluster_noContainersFound() {
        MesosCluster.loadCluster("nonexistent", new MesosClusterContainersFactory());
    }

    @Test
    public void mesosAgentStateInfoJSONMatchesSchema() throws UnirestException, JsonParseException, JsonMappingException {
        String agentId = CLUSTER.getAgents().get(0).getContainerId();
        JSONObject state = CLUSTER.getAgentStateInfo(agentId);
        Assert.assertNotNull(state);
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        MesosMaster master = CLUSTER.getMaster();
        State state = master.getState();

        Assert.assertEquals(3, state.getActivatedAgents());
    }

    @Test
    public void mesosResourcesCorrect() throws Exception {
        JSONObject stateInfo = CLUSTER.getMaster().getStateInfoJSON();
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals((long) 1, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getLong("cpus"));
            Assert.assertEquals(256, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getInt("mem"));
        }
    }

    @Test
    public void testAgentStateRetrieval() throws UnsupportedEncodingException {
        List<MesosAgent> agents = CLUSTER.getAgents();
        Assert.assertNotNull(agents);
        Assert.assertTrue(agents.size() > 0);

        MesosAgent agent = agents.get(0);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(outputStream, true);

        String cliContainerId = agent.getContainerId().substring(0, 11);

        CLUSTER.state(ps, cliContainerId);

        String state = outputStream.toString("UTF-8");
        Assert.assertTrue(state.contains("frameworks"));
        Assert.assertTrue(state.contains("resources"));
    }

    @Test
    public void dockerExposeResourcesPorts() throws Exception {
        List<MesosAgent> containers = CLUSTER.getAgents();

        for (MesosAgent container : containers) {
            ArrayList<Integer> ports = ResourceUtil.parsePorts(container.getResources());
            InspectContainerResponse response = DockerClientFactory.build().inspectContainerCmd(container.getContainerId()).exec();
            Map bindings = response.getNetworkSettings().getPorts().getBindings();
            for (Integer port : ports) {
                Assert.assertTrue(bindings.containsKey(new ExposedPort(port)));
            }
        }
    }

    @Test
    public void testPullAndStartContainer() throws UnirestException {
        HelloWorldContainer container = new HelloWorldContainer();
        String containerId = CLUSTER.addAndStartProcess(container);
        String ipAddress = DockerContainersUtil.getIpAddress(containerId);
        String url = "http://" + ipAddress + ":" + HelloWorldContainer.SERVICE_PORT;
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    @Test
    public void testMasterLinkedToAgents() throws UnirestException {
        List<MesosAgent> containers = CLUSTER.getAgents();
        for (MesosAgent container : containers) {
            InspectContainerResponse exec = DockerClientFactory.build().inspectContainerCmd(container.getContainerId()).exec();

            List<Link> links = Arrays.asList(exec.getHostConfig().getLinks());

            Assert.assertNotNull(links);
            Assert.assertEquals("link to zookeeper is expected", 1, links.size());
            Assert.assertEquals("minimesos-zookeeper", links.get(0).getAlias());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testStartingClusterSecondTime() {
        CLUSTER.start(30);
    }

}
