package com.containersol.minimesos.config

import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*

public class ConfigWriterTest {

    private ConfigParser parser

    @Before
    public void before() {
        parser = new ConfigParser()
    }

    @Test
    public void testWritingDefaultConfiguration() {
        ClusterConfig config = new ClusterConfig()
        String strConfig = parser.toString(config)
        ClusterConfig read = parser.parse(strConfig)
        compareClusters(config, read)
    }

    @Test
    public void testWritingFilledConfiguration() {

        ClusterConfig config = new ClusterConfig()

        config.master = new MesosMasterConfig()
        config.zookeeper = new ZooKeeperConfig()
        config.marathon = new MarathonConfig()
        config.agents.add(new MesosAgentConfig())
        config.consul = new ConsulConfig()
        config.registrator = new RegistratorConfig()

        AppConfig appConfig = new AppConfig()
        appConfig.setMarathonJson("http://www.google.com")
        config.marathon.apps.add(appConfig)

        AppConfig fileAppConfig = new AppConfig()
        fileAppConfig.setMarathonJson("/temp/fileA")
        config.marathon.apps.add(fileAppConfig)

        String strConfig = parser.toString(config)
        ClusterConfig read = parser.parse(strConfig)

        compareClusters(config, read)
        assertNotNull("Marathon container must be set", read.marathon)
        assertEquals(config.marathon.apps.size(), read.marathon.apps.size())
        assertEquals("http://www.google.com", read.marathon.apps[0].marathonJson)
        assertEquals("/temp/fileA", read.marathon.apps[1].marathonJson)

    }

    static private void compareClusters(ClusterConfig first, ClusterConfig second) {

        assertEquals(first.timeout, second.timeout)
        assertEquals(first.clusterName, second.clusterName)
        assertEquals(first.loggingLevel, second.loggingLevel)

        compareContainers(first.marathon, second.marathon)

        compareContainers(first.zookeeper, second.zookeeper)
        compareContainers(first.consul, second.consul)
        compareContainers(first.registrator, second.registrator)

        compareMesosContainers(first.master, second.master)

        assertEquals(first.agents.size(), second.agents.size())
        if (first.agents.size() > 0) {
            compareMesosContainers(first.agents[0], second.agents[0])
        }

    }

    static void compareContainers(ContainerConfig first, ContainerConfig second) {
        if (first == null) {
            if (second != null) {
                fail("Expected null, but found " + second)
            }
        } else {
            if (second == null) {
                fail("Expected " + first + ", but null was found")
            } else {
                assertEquals(first.imageName, second.imageName)
                assertEquals(first.imageTag, second.imageTag)
            }
        }
    }

    static void compareMesosContainers(MesosContainerConfig first, MesosContainerConfig second) {
        compareContainers(first, second)
        if (first != null) {
            assertEquals(first.loggingLevel, second.loggingLevel)
        }
    }

}
