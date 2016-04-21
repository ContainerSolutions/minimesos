package com.containersol.minimesos;

import com.containersol.minimesos.cluster.Filter;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HostNetworkingTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-hostNetworkingTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void testHostNetworking() {
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getMaster().getIpAddress());
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getAgents().get(0).getIpAddress());
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getMarathon().getIpAddress());
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getConsul().getIpAddress());
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getZooKeeper().getIpAddress());
        assertEquals(DockerContainersUtil.getGatewayIpAddress(), CLUSTER.getOne(Filter.registrator()).get().getIpAddress());
    }

}
