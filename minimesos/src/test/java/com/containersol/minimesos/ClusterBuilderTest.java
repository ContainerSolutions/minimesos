package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.docker.MesosAgentContainer;
import com.containersol.minimesos.docker.MesosClusterDockerFactory;
import com.containersol.minimesos.util.CollectionsUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ClusterBuilderTest {

    @Test
    public void testInheritedImageTag() {
        String config = "minimesos { \n" +
                "mesosVersion = \"0.26\" \n" +
                "agent { imageTag = \"0.27.0-0.1.0\"} \n" +
                "agent {} \n" +
                "}";

        ConfigParser parser = new ConfigParser();
        ClusterConfig dsl = parser.parse(config);

        MesosCluster cluster = new MesosClusterDockerFactory().createMesosCluster(dsl);

        List<MesosAgentContainer> agents = CollectionsUtils.typedList(cluster.getAgents(), MesosAgentContainer.class);
        assertEquals(2, agents.size());

        assertEquals("0.27.0-" + ClusterConfig.DEFAULT_MINIMESOS_DOCKER_VERSION, agents.get(0).getImageTag());
        assertEquals("0.26-" + ClusterConfig.DEFAULT_MINIMESOS_DOCKER_VERSION, agents.get(1).getImageTag());
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

        MesosCluster cluster = new MesosClusterDockerFactory().createMesosCluster(dsl);

        List<MesosAgentContainer> agents = CollectionsUtils.typedList(cluster.getAgents(), MesosAgentContainer.class);
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

        MesosCluster cluster = new MesosClusterDockerFactory().createMesosCluster(dsl);

        List<MesosAgentContainer> agents = CollectionsUtils.typedList(cluster.getAgents(), MesosAgentContainer.class);
        assertEquals(2, agents.size());

        assertEquals("ERROR", agents.get(0).getLoggingLevel());
        assertEquals("WARNING", agents.get(1).getLoggingLevel());
    }

}
