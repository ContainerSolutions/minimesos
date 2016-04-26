package com.containersol.minimesos.main;

import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandInfoTest {

    private CommandInfo info = null;

    @Before
    public void before() {
        info = new CommandInfo();
    }

    @Test
    public void testGetDistinctRoleProcesses() throws Exception {

        ClusterProcess master = new AbstractContainer(new MesosMasterConfig()) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "master";
            }
        };
        ClusterProcess consul = new AbstractContainer(new ConsulConfig()) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "consul";
            }
        };
        ClusterProcess agent1 = new AbstractContainer(new MesosAgentConfig()) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "agent";
            }
        };
        ClusterProcess agent2 = new AbstractContainer(new MesosAgentConfig()) {
            @Override
            protected CreateContainerCmd dockerCommand() {
                return null;
            }

            @Override
            public String getRole() {
                return "agent";
            }
        };

        List<ClusterProcess> processes = Arrays.asList(master, consul, agent1, agent2);
        List<ClusterProcess> distinct = info.getDistinctRoleProcesses(processes);

        assertEquals(2, distinct.size());
        assertTrue("master has a distinct role", distinct.contains(master));
        assertTrue("consul has a distinct role", distinct.contains(consul));

    }

}
