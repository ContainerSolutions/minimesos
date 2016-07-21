package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RestoreConfigurationFromRunningClusterTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-restoreConfigTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void testMapPortsToHostRestored() {
        String clusterId = CLUSTER.getClusterId();
        MesosCluster cluster = MesosCluster.loadCluster(clusterId, new MesosClusterContainersFactory());

        assertTrue("Deserialize cluster is expected to remember mapPortsToHost setting", cluster.isMapPortsToHost());
    }

    @Test
    public void testMesosVersionRestored() {
        String clusterId = CLUSTER.getClusterId();
        MesosCluster cluster = MesosCluster.loadCluster(clusterId, new MesosClusterContainersFactory());

        assertEquals("0.25.0", cluster.getMesosVersion());
    }

}
