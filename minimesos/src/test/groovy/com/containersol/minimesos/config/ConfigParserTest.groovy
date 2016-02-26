package com.containersol.minimesos.config

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

        parser.parse(config)

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

        parser.parse(config)

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

        parser.parse(config)

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.marathon)
        assertEquals("mesosphere/marathon", dsl.marathon.imageName)
        assertEquals("v0.13.0", dsl.marathon.imageTag)
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

        parser.parse(config)

        ClusterConfig dsl = parser.parse(config)
        assertNotNull(dsl.marathon)
        assertEquals("containersol/marathon", dsl.marathon.imageName)
        assertEquals("v0.14.0", dsl.marathon.imageTag)
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
