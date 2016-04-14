package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import org.junit.ClassRule;
import org.junit.Ignore;

import static org.junit.Assert.assertEquals;

public class HostNetworkingTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-hostNetworkingTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Ignore("Cannot determine gateway IP at the moment")
    public void testHostNetworking() {
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getAgents().get(0).getIpAddress());
    }

}
