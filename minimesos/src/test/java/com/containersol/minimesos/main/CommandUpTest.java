package com.containersol.minimesos.main;

import com.containersol.minimesos.cluster.*;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.ClusterContainers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class CommandUpTest {

    @Test
    public void testDefaultClusterConfig() throws IOException {
        CommandUp commandUp = new CommandUp();

        ClusterArchitecture architecture = commandUp.getClusterArchitecture();
        assertNotNull("architecture is not loaded", architecture);

        ClusterContainers clusterContainers = architecture.getClusterContainers();
        assertNotNull("cluster containers are not loaded", clusterContainers);

        assertTrue("ZooKeeper is required component of cluster", clusterContainers.isPresent(Filter.zooKeeper()));
        assertTrue("Mesos Master is required component of cluster", clusterContainers.isPresent(Filter.mesosMaster()));
    }

    @Test
    public void testBasicClusterConfig() throws IOException {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/test/resources/clusterconfig/basic.groovy");

        ClusterArchitecture architecture = commandUp.getClusterArchitecture();
        assertNotNull("architecture is not loaded", architecture);

        ClusterContainers clusterContainers = architecture.getClusterContainers();
        assertNotNull("cluster containers are not loaded", clusterContainers);

        assertTrue("ZooKeeper is required component of cluster", clusterContainers.isPresent(Filter.zooKeeper()));
        assertTrue("Mesos Master is required component of cluster", clusterContainers.isPresent(Filter.mesosMaster()));

        List<MesosAgent> agents = clusterContainers.getContainers().stream().filter(Filter.mesosAgent()).map(c -> (MesosAgent) c).collect(Collectors.toList());
        assertEquals(1, agents.size());
    }

    @Test
    public void testTwoAgentsClusterConfig() throws IOException {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/test/resources/clusterconfig/two-agents.groovy");

        ClusterArchitecture architecture = commandUp.getClusterArchitecture();
        assertNotNull("architecture is not loaded", architecture);

        ClusterContainers clusterContainers = architecture.getClusterContainers();
        assertNotNull("cluster containers are not loaded", clusterContainers);

        assertTrue("ZooKeeper is required component of cluster", clusterContainers.isPresent(Filter.zooKeeper()));
        assertTrue("Mesos Master is required component of cluster", clusterContainers.isPresent(Filter.mesosMaster()));

        List<MesosAgent> agents = clusterContainers.getContainers().stream().filter(Filter.mesosAgent()).map(c -> (MesosAgent) c).collect(Collectors.toList());
        assertEquals(2, agents.size());
    }

    @Test
    public void testUsingExposedPortsFromConfigFileTrue() {
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setExposePorts(true);

        CommandUp commandUp = new CommandUp();
        commandUp.updateWithParameters(clusterConfig);

        assertTrue("Exposed port from configuration is expected to remain", clusterConfig.isExposePorts());
    }

    @Test
    public void testUsingExposedPortsFromConfigFileFalse() {
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setExposePorts(false);

        CommandUp commandUp = new CommandUp();
        commandUp.updateWithParameters(clusterConfig);

        assertFalse("Exposed port from configuration is expected to remain", clusterConfig.isExposePorts());
    }

    @Test
    public void testOverwritingExposedPortsFromCommand() {
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setExposePorts(true);

        CommandUp commandUp = new CommandUp();
        commandUp.setExposedHostPorts(false);
        commandUp.updateWithParameters(clusterConfig);

        assertFalse("Exposed port should be changed through command parameters", clusterConfig.isExposePorts());
    }

    @Test
    public void testMinimalConfig() throws InterruptedException {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/test/resources/configFiles/minimal-minimesosFile");

        ClusterArchitecture clusterArchitecture = commandUp.getClusterArchitecture();

        assertEquals(1, clusterArchitecture.getClusterContainers().getContainers().stream().filter(c -> c instanceof ZooKeeper).count());
        assertEquals(1, clusterArchitecture.getClusterContainers().getContainers().stream().filter(c -> c instanceof MesosMaster).count());
        assertEquals(1, clusterArchitecture.getClusterContainers().getContainers().stream().filter(c -> c instanceof MesosAgent).count());
        assertEquals(3, clusterArchitecture.getClusterContainers().getContainers().size());
    }

    @Test
    public void testMarathonAppConfig() throws InterruptedException {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/test/resources/configFiles/marathonAppConfig-minimesosFile");

        commandUp.execute();

        MesosCluster cluster = commandUp.getCluster();

        await().atMost(60, TimeUnit.SECONDS).until(() -> {
            try {
                JSONObject state = cluster.getClusterStateInfo();
                JSONObject marathon = state.getJSONArray("frameworks").getJSONObject(0);
                return marathon.getJSONArray("completed_tasks").getJSONObject(0).getString("name").equals("hello");
            } catch (JSONException e) {
                return false;
            }
        });

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();
    }

}
