package com.containersol.minimesos.marathon;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MarathonContainerTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-mesosClusterTest");

    public static final MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void testFindMesosMaster() {
        String initString = "start ${MINIMESOS_MASTER} ${MINIMESOS_MASTER_IP} end";

        String expected = CLUSTER.getMaster().getServiceUrl().toString();
        String ip = CLUSTER.getMaster().getIpAddress();

        MarathonContainer marathon = (MarathonContainer) CLUSTER.getMarathon();
        String updated = marathon.replaceTokens(initString);
        assertEquals("MINIMESOS_MASTER should be replaced", String.format("start %s %s end", expected, ip), updated);
    }

}
