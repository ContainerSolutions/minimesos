package com.containersol.minimesos;


import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.MesosAgent;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClusterBuilderTest {

    @Test
    public void testInheritedLoggingLevel() {

        String config = "minimesos { \n" +
                "loggingLevel = \"warning\" \n" +
                "agent { loggingLevel = \"ERROR\"} \n" +
                "agent {} \n" +
                "}";

        ConfigParser parser = new ConfigParser();
        ClusterConfig dsl = parser.parse(config);

        ClusterArchitecture.Builder builder = ClusterArchitecture.Builder.createCluster(dsl);
        MesosCluster cluster = new MesosCluster(builder.build());

        List<MesosAgent> agents = cluster.getAgents();
        assertEquals(2, agents.size());

        assertEquals("ERROR", agents.get(0).getLoggingLevel());
        assertEquals("WARNING", agents.get(1).getLoggingLevel());

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

        ClusterArchitecture.Builder builder = ClusterArchitecture.Builder.createCluster(dsl);
        MesosCluster cluster = new MesosCluster(builder.build());

        List<MesosAgent> agents = cluster.getAgents();
        assertEquals(2, agents.size());

        assertEquals("ERROR", agents.get(0).getLoggingLevel());
        assertEquals("INFO", agents.get(1).getLoggingLevel());

    }

}
