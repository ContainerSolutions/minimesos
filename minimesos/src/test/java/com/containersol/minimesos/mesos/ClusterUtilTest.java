package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosAgent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests helper methods
 */
public class ClusterUtilTest {
    @Test
    public void shouldBeAbleToAddAgents() {
        ClusterArchitecture.Builder builder = ClusterUtil.withAgent(3);
        assertEquals(3 + 1 + 1, builder.build().getClusterContainers().getContainers().size());
    }

    @Test
    public void shouldBeAbleToAddCustomAgents() {
        MesosAgent mock = mock(MesosAgent.class);
        ClusterArchitecture.Builder builder = ClusterUtil.withAgent(3, zk -> mock);
        assertEquals(3 + 1 + 1, builder.build().getClusterContainers().getContainers().size());
    }
}
