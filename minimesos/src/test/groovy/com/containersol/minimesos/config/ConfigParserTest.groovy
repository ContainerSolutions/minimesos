package com.containersol.minimesos.config

import com.containersol.minimesos.MinimesosException
import com.containersol.minimesos.cluster.MesosCluster
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull;

public class ConfigParserTest {

    private ConfigParser parser

    @Before
    public void before() {
        parser = new ConfigParser()
    }

    @Test
    public void testLoggingLevel() {
        String config =
                """
        minimesos {

            loggingLevel = "WARNING"

        }
        """

        assertEquals("WARNING", parser.parse(config).getLoggingLevel())
    }

    @Test
    public void testLoggingLevel_caseInsensitive() {
        String config =
                """
        minimesos {

            loggingLevel = "warning"

        }
        """

        assertEquals("WARNING", parser.parse(config).getLoggingLevel())
    }

    @Test
    public void testClusterName() {
        String config =
                """
        minimesos {

            clusterName = "testcluster"

        }
        """

        assertEquals("testcluster", parser.parse(config).getClusterName())
    }

    @Test
    public void testTimeout() {
        String config =
                """
        minimesos {

            timeout = 500

        }
        """

        assertEquals(500, parser.parse(config).getTimeout())
    }

    @Test(expected = MissingPropertyException.class)
    public void testUnsupportedProperty() {
        String config =
                """
        minimesos {

            unsupportedProperty = "foo"

        }
        """

        parser.parse(config)
    }

    @Test(expected = MissingPropertyException.class)
    public void testUnsupportedBlock() {
        String config =
                """
        minimesos {

            unsupportedBlock {

            }

        }
        """

        parser.parse(config)
    }

    @Test
    public void testLoadSingleAgent() {
        String config = """
                minimesos {
                    agent {
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertEquals(1, dsl.agents.size())

        MesosAgentConfig agent = dsl.agents.get(0)
        assertNotNull(agent)
    }

    @Test
    public void testLoadTwoAgents() {
    String config = """
                minimesos {
                    agent {
                    }
                    agent {
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertEquals(2, dsl.agents.size())
    }

    @Test
    public void testLoadAgentTwoAgents_loggingLevel() {
        String config = """
                minimesos {

                    loggingLevel = "warning"

                    agent {
                        loggingLevel = "ERROR"
                    }

                    agent {

                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)

        MesosAgentConfig agent1 = dsl.agents.get(0)
        assertEquals("ERROR", agent1.getLoggingLevel())

        MesosAgentConfig agent2 = dsl.agents.get(1)
        assertEquals(MesosContainerConfig.MESOS_LOGGING_LEVEL_INHERIT, agent2.getLoggingLevel())
    }

    @Test
    public void testLoadMaster() {
        String config = """
                minimesos {
                    master {
                        imageName  = "another/master"
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.master)
        assertEquals("another/master", dsl.master.imageName)
    }

    @Test(expected = RuntimeException.class)
    public void testMesosVersion_nonExistentVersion() {
        String config = """
                minimesos {

                    mesosVersion = "1.0.0-does-not-exist"

                    master {

                    }
                }
        """

        parser.parse(config)
    }

    @Test
    public void testMesosVersion_inheritTag() {
        String config = """
                minimesos {

                    mesosVersion = "0.26"

                    master {

                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.master)
        assertEquals("containersol/mesos-master", dsl.master.imageName)
        assertEquals( MesosContainerConfig.MESOS_IMAGE_TAG, dsl.master.imageTag)
    }

    @Test
    public void testMesosVersion_overrideTag() {
        String config = """
                minimesos {

                    mesosVersion = "0.26"

                    master {
                        imageTag = "0.27"
                    }

                    agent {
                        imageTag = "0.28"
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.master)
        assertEquals("containersol/mesos-master", dsl.master.imageName)
        assertEquals("0.27", dsl.master.imageTag)

        assertNotNull(dsl.agents.get(0))
        assertEquals("containersol/mesos-agent", dsl.agents.get(0).imageName)
        assertEquals("0.28", dsl.agents.get(0).imageTag)
    }

    @Test(expected = Exception.class)
    public void testFailureToLoadTwoMaster() {
        String config = """
                minimesos {
                    master {
                        imageName  = "another/master"
                    }
                    master {
                    }
                }
        """

        parser.parse(config)
    }

    @Test
    public void testZookeeper() {
        String config = """
                minimesos {
                    zookeeper {

                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.zookeeper)
        assertEquals("jplock/zookeeper", dsl.zookeeper.imageName)
        assertEquals("3.4.6", dsl.zookeeper.imageTag)
    }

    @Test
    public void testZookeeper_properties() {
        String config = """
                minimesos {
                    zookeeper {
                      imageName = "containersol/zookeeper"
                      imageTag  = "3.4.5"
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.zookeeper)
        assertEquals("containersol/zookeeper", dsl.zookeeper.imageName)
        assertEquals("3.4.5", dsl.zookeeper.imageTag)
    }

    @Test
    public void testMarathon() {
        String config = """
                minimesos {
                    marathon {

                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.marathon)
        assertEquals("mesosphere/marathon", dsl.marathon.imageName)
        assertEquals("v0.15.3", dsl.marathon.imageTag)
    }

    @Test
    public void testMarathon_properties() {
        String config = """
                minimesos {
                    marathon {
                      imageName = "containersol/marathon"
                      imageTag  = "v0.14.0"
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.marathon)
        assertEquals("containersol/marathon", dsl.marathon.imageName)
        assertEquals("v0.14.0", dsl.marathon.imageTag)
    }

    @Test(expected = MinimesosException.class)
    public void testMarathonApp_noUrlOrPath() {
        String config = """
                minimesos {
                    marathon {
                        app {
    
                        }
                    }
                }
        """

        parser.parse(config)
    }

    @Test
    public void testMarathonApp_path() {
        String config = """
                minimesos {
                    marathon {
                        app {
                            marathonJsonPath = "src/test/resources/app.json"
                        }
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.marathon)
        assertEquals(MesosCluster.getHostDir().getAbsolutePath() + "/src/test/resources/app.json", dsl.marathon.getApps().get(0).file.getAbsolutePath())
    }

    @Test(expected = MinimesosException.class)
    public void testMarathonApp_incorrectPath() {
        String config = """
                minimesos {
                    marathon {
                        app {
                            marathonJsonPath = "nonExistingFile.json"
                        }
                    }
                }
        """

        parser.parse(config)
    }

    @Test
    public void testMarathonApp_url() {
        String config = """
                minimesos {
                    marathon {
                        app {
                            marathonJsonUrl = "https://www.github.com/organization/repo/app.json"
                        }
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.marathon)
        assertEquals("https://www.github.com/organization/repo/app.json", dsl.marathon.getApps().get(0).url.toString())
    }

    @Test(expected = MinimesosException.class)
    public void testMarathonApp_incorrectUrl() {
        String config = """
                minimesos {
                    marathon {
                        app {
                            marathonJsonUrl = "incorrectUrl"
                        }
                    }
                }
        """

        parser.parse(config)
    }

    @Test(expected = MinimesosException.class)
    public void testMarathonApp_urlOrPath() {
        String config = """
                minimesos {
                    marathon {
                        app {
                            marathonJsonUrl     = "https://www.github.com/organization/repo/app.json"
                            marathonJsonPath    = "src/test/resources/app.json"
                        }
                    }
                }
        """

        parser.parse(config)
    }

    @Test
    public void testLoadSingleAgentResourcesNumbers() {
        String config = """
                minimesos {
                    agent {
                        resources {
                            cpu {
                                role  = "logstash"
                                value = 1
                            }
                            cpu {
                                role  = "*"
                                value = 4
                            }
                            ports {
                                role  = "logstash"
                                value = "[514-514]"
                            }
                        }
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertEquals(1, dsl.agents.size())

        MesosAgentConfig agent = dsl.agents.get(0)

        assertEquals(4, agent.resources.cpus["*"].value, 0.0001)
        assertEquals(1, agent.resources.cpus["logstash"].value, 0.0001)

        assertNotNull(agent.resources.mems["*"])
        assertNull(agent.resources.mems["logstash"])

        assertNotNull(agent.resources.ports["*"])
        assertEquals("[514-514]", agent.resources.ports["logstash"].value)
    }

    @Test
    /**
     * Explicit test for surrounding numbers with ""
     */
    public void testLoadSingleAgentResourcesStrings() {
        String config = """
                minimesos {
                    agent {
                        resources {
                            cpu {
                                role  = "logstash"
                                value = "1"
                            }
                            cpu {
                                role  = "*"
                                value = "4"
                            }
                            ports {
                                role  = "logstash"
                                value = "[514-514]"
                            }
                        }
                    }
                }
        """

        ClusterConfig dsl = parser.parse(config)
        assertEquals(1, dsl.agents.size())

        MesosAgentConfig agent = dsl.agents.get(0)

        assertEquals(4, agent.resources.cpus["*"].value, 0.0001)
        assertEquals(1, agent.resources.cpus["logstash"].value, 0.0001)

        assertNotNull(agent.resources.mems["*"])
        assertNull(agent.resources.mems["logstash"])

        assertNotNull(agent.resources.ports["*"])
        assertEquals("[514-514]", agent.resources.ports["logstash"].value)
    }

}
