package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.config.MarathonConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.Consul;
import com.containersol.minimesos.mesos.MesosAgent;
import com.containersol.minimesos.mesos.MesosMaster;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ExposedPortsTest {
    private static final boolean EXPOSED_PORTS = true;

    private MesosCluster cluster;

    @Before
    public void beforeTest() {

        MesosMasterConfig masterConfig = new MesosMasterConfig();
        MesosAgentConfig agentConfig = new MesosAgentConfig();
        MarathonConfig marathonConfig = new MarathonConfig();
        ConsulConfig consulConfig = new ConsulConfig();

        ClusterArchitecture architecture = new ClusterArchitecture.Builder()
                .withZooKeeper()
                .withMaster(zooKeeper -> new MesosMaster(zooKeeper, masterConfig))
                .withAgent(zooKeeper -> new MesosAgent(zooKeeper, agentConfig))
                .withMarathon(zooKeeper -> new Marathon(zooKeeper, marathonConfig))
                .withConsul(new Consul(consulConfig))
                .build();

        cluster = new MesosCluster(architecture);
        cluster.setExposedHostPorts(EXPOSED_PORTS);
        cluster.start();

    }

    @After
    public void afterTest() {
        if (cluster != null) {
            cluster.stop();
        }
    }

    @Test
    public void testLoadCluster() {
        String clusterId = cluster.getClusterId();
        MesosCluster cluster = MesosCluster.loadCluster(clusterId);

        assertTrue("Deserialize cluster is expected to remember exposed ports setting", cluster.isExposedHostPorts());
    }

}
