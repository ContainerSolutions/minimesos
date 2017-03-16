package com.containersol.minimesos.integrationtest;

import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.MesosClusterFactory;
import com.containersol.minimesos.cluster.MesosMaster;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.integrationtest.container.HelloWorldContainer;
import com.containersol.minimesos.integrationtest.container.MesosExecuteContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersol.minimesos.docker.MarathonContainer;
import com.containersol.minimesos.docker.MesosAgentContainer;
import com.containersol.minimesos.docker.MesosClusterDockerFactory;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.util.Environment;
import com.containersol.minimesos.util.ResourceUtil;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.jayway.awaitility.Awaitility;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.junit.*;

import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class MesosClusterTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-mesosClusterTest");

    public static final MesosCluster CLUSTER = RULE.getMesosCluster();

    @After
    public void after() {
        DockerContainersUtil.getContainers(false).filterByName(HelloWorldContainer.CONTAINER_NAME_PATTERN).kill().remove();
    }

    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        MesosMaster master = CLUSTER.getMaster();
        State state = master.getState();

        assertEquals(3, state.getActivatedAgents());
    }

    @Test
    public void mesosResourcesCorrect() throws Exception {
        JSONObject stateInfo = CLUSTER.getMaster().getStateInfoJSON();
        for (int i = 0; i < 3; i++) {
            assertEquals((long) 4, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getLong("cpus"));
            assertEquals(512, stateInfo.getJSONArray("slaves").getJSONObject(0).getJSONObject("resources").getInt("mem"));
        }
    }

    @Test
    public void dockerExposeResourcesPorts() throws Exception {
        List<MesosAgent> containers = CLUSTER.getAgents();

        for (MesosAgent container : containers) {
            ArrayList<Integer> ports = ResourceUtil.parsePorts(container.getResources());
            InspectContainerResponse response = DockerClientFactory.build().inspectContainerCmd(container.getContainerId()).exec();
            Map bindings = response.getNetworkSettings().getPorts().getBindings();
            for (Integer port : ports) {
                assertTrue(bindings.containsKey(new ExposedPort(port)));
            }
        }
    }

    @Test
    public void testHelloWorldContainer() throws UnirestException {
        Assume.assumeFalse("Only test hello world container on Linux", Environment.isRunningInJvmOnMacOsX());
        HelloWorldContainer container = new HelloWorldContainer();
        container.start(60);
        URI url = container.getServiceUrl();
        assertEquals(200, Unirest.get(url.toString()).asString().getStatus());
    }

    @Test
    public void testMasterLinkedToAgents() throws UnirestException {
        List<MesosAgent> containers = CLUSTER.getAgents();
        for (MesosAgent container : containers) {
            InspectContainerResponse exec = DockerClientFactory.build().inspectContainerCmd(container.getContainerId()).exec();

            List<Link> links = Arrays.asList(exec.getHostConfig().getLinks());

            assertNotNull(links);
            assertEquals("link to zookeeper is expected", 1, links.size());
            assertEquals("minimesos-zookeeper", links.get(0).getAlias());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testStartingClusterSecondTime() {
        CLUSTER.start(30);
    }

    @Test
    public void testMesosVersionRestored() {
        String clusterId = CLUSTER.getClusterId();

        MesosClusterFactory factory = new MesosClusterDockerFactory();
        MesosCluster cluster = factory.retrieveMesosCluster(clusterId);

        assertEquals("1.0.0", cluster.getConfiguredMesosVersion());
    }

    @Test
    public void testFindMesosMaster() {
        Assume.assumeFalse("Only test token interpolation on Linux", Environment.isRunningInJvmOnMacOsX());

        String initString = "start ${MINIMESOS_MASTER} ${MINIMESOS_MASTER_IP} end";

        String expected = CLUSTER.getMaster().getServiceUrl().toString();
        String ip = CLUSTER.getMaster().getIpAddress();

        MarathonContainer marathon = (MarathonContainer) CLUSTER.getMarathon();
        String updated = marathon.replaceTokens(initString);
        assertEquals("MINIMESOS_MASTER should be replaced", String.format("start %s %s end", expected, ip), updated);
    }

    private static class LogContainerTestCallback extends LogContainerResultCallback {
        final StringBuffer log = new StringBuffer();

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
        ClusterProcess mesosExecute = new MesosExecuteContainer();

        String containerId = CLUSTER.addAndStartProcess(mesosExecute);

        Awaitility.await("Mesos Execute container did not start responding").atMost(60, TimeUnit.SECONDS).until(() -> {
            LogContainerTestCallback cb1 = new LogContainerTestCallback();
            DockerClientFactory.build().logContainerCmd(mesosExecute.getContainerId()).withContainerId(containerId).withStdOut(true).exec(cb1);
            cb1.awaitCompletion();
            String log = cb1.toString();
            return log.contains("Received status update TASK_FINISHED for task 'test-cmd'");
        });
    }

    @Test
    public void noMarathonTest() throws FileNotFoundException {
        String clusterId = CLUSTER.getClusterId();

        assertNotNull("Cluster ID must be set", clusterId);

        // this should not throw any exceptions
        CLUSTER.destroy(RULE.getFactory());
    }

    @Test
    public void stopWithNewContainerTest() {
        MesosAgent extraAgent = new MesosAgentContainer(new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION));
        ZooKeeper zooKeeper = CLUSTER.getZooKeeper();
        extraAgent.setZooKeeper(zooKeeper);

        String containerId = CLUSTER.addAndStartProcess(extraAgent);
        assertNotNull("freshly started container is not found", DockerContainersUtil.getContainer(containerId));

        CLUSTER.destroy(RULE.getFactory());
        assertNull("new container should be stopped too", DockerContainersUtil.getContainer(containerId));
    }

    @Test
    public void testStartTwiceShouldNoOp() {
        MesosMaster master = CLUSTER.getMaster();
        master.start(5);
    }

}
