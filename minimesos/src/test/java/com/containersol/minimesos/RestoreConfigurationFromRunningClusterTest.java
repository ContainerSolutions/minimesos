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
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromClassPath("/configFiles/minimesosFile-mapPortsToHostTest");

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

        assertEquals("0.27", cluster.getMesosVersion());
    }

}
