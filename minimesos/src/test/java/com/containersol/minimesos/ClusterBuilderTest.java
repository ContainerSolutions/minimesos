package com.containersol.minimesos;


import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClusterBuilderTest {

    @Test
    public void testInheritedImageTag() {
        String config = "minimesos { \n" +
                "mesosVersion = \"0.26\" \n" +
                "agent { imageTag = \"0.27.0-0.2.190.ubuntu1404\"} \n" +
                "agent {} \n" +
                "}";

        ConfigParser parser = new ConfigParser();
        ClusterConfig dsl = parser.parse(config);

        ClusterArchitecture.Builder builder = ClusterArchitecture.Builder.createCluster(dsl);
        ClusterArchitecture architecture = builder.build();
        MesosCluster cluster = new MesosCluster(architecture.getClusterConfig(), architecture.getClusterContainers().getContainers());

        List<MesosAgent> agents = cluster.getAgents();
        assertEquals(2, agents.size());

        assertEquals("0.27.0-0.2.190.ubuntu1404", agents.get(0).getMesosImageTag());
        assertEquals("0.26.0-0.2.145.ubuntu1404", agents.get(1).getMesosImageTag());
    }

    @Test
    public void testDefaultInAgentLoggingLevel() {
        String config = "minimesos { \n" +
                "loggingLevel = \"warning\" \n" +
                "agent { loggingLevel = \"ERROR\" } \n" +
                "agent { loggingLevel = \"INFO\" } \n" +
                "}";

        ConfigParser parser = new ConfigParser();
        ClusterConfig dsl = parser.parse(config);

        ClusterArchitecture architecture = ClusterArchitecture.Builder.createCluster(dsl).build();
        MesosCluster cluster = new MesosCluster(architecture.getClusterConfig(), architecture.getClusterContainers().getContainers());

        List<MesosAgent> agents = cluster.getAgents();
        assertEquals(2, agents.size());

        assertEquals("ERROR", agents.get(0).getLoggingLevel());
        assertEquals("INFO", agents.get(1).getLoggingLevel());
    }

    @Test
    public void testInheritedLoggingLevel() {
        String config = "minimesos { \n" +
                "loggingLevel = \"warning\" \n" +
                "agent { loggingLevel = \"ERROR\"} \n" +
                "agent {} \n" +
                "}";

        ConfigParser parser = new ConfigParser();
        ClusterConfig dsl = parser.parse(config);

        ClusterArchitecture architecture = ClusterArchitecture.Builder.createCluster(dsl).build();
        MesosCluster cluster = new MesosCluster(architecture.getClusterConfig(), architecture.getClusterContainers().getContainers());

        List<MesosAgent> agents = cluster.getAgents();
        assertEquals(2, agents.size());

        assertEquals("ERROR", agents.get(0).getLoggingLevel());
        assertEquals("WARNING", agents.get(1).getLoggingLevel());
    }

}
