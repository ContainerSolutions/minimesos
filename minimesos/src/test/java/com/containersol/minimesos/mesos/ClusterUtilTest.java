package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterUtil;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.integrationtest.container.AbstractContainer;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests helper methods
 */
public class ClusterUtilTest {

    @Test
    public void testGetDistinctRoleProcesses() throws Exception {

        ClusterProcess master = new AbstractContainer(new MesosMasterConfig(ClusterConfig.DEFAULT_MESOS_VERSION)) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "master";
            }
        };
        ClusterProcess agent1 = new AbstractContainer(new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION)) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "agent";
            }
        };
        ClusterProcess agent2 = new AbstractContainer(new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION)) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "agent";
            }
        };

        List<ClusterProcess> processes = Arrays.asList(master, agent1, agent2);
        List<ClusterProcess> distinct = ClusterUtil.getDistinctRoleProcesses(processes);

        assertEquals(2, distinct.size());
        assertTrue("master has a distinct role", distinct.contains(master));

    }
}
