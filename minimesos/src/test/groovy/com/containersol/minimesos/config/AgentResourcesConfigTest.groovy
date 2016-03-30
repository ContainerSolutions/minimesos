package com.containersol.minimesos.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class AgentResourcesConfigTest {

    @Test
    public void testDefaultResourcesAsString() {

        AgentResourcesConfig resources = new AgentResourcesConfig()
        String asString = resources.asMesosString()
        String expected = "ports(*):[31000-32000]; cpus(*):1; mem(*):256; disk(*):200"

        assertEquals(expected, asString)

    }

    @Test
    public void testPortsFromString() {

        String strResources = "ports(*):[8081-8082]"
        AgentResourcesConfig resources = AgentResourcesConfig.fromString(strResources);

        assertEquals(0, resources.cpus.size())
        assertEquals(0, resources.disks.size())
        assertEquals(0, resources.mems.size())

        assertEquals(1, resources.ports.size())
        assertEquals("[8081-8082]", String.valueOf(resources.ports["*"].value))

    }

    @Test
    public void testPortsCpusFromString() {

        String strResources = "ports(*):[8081-8082]; cpus(*):1.2"
        AgentResourcesConfig resources = AgentResourcesConfig.fromString(strResources);

        assertEquals(1, resources.cpus.size())
        double actual = resources.cpus["*"].value
        assertEquals(1.2, actual, 0.001)

        assertEquals(0, resources.disks.size())
        assertEquals(0, resources.mems.size())

        assertEquals(1, resources.ports.size())
        assertEquals("[8081-8082]", resources.ports["*"].value)

    }

    @Test
    public void testElasticSearchResources() {

        String resources = "ports(testRole):[9200-9200,9300-9300]; cpus(testRole):0.2; mem(testRole):256; disk(testRole):200"
        AgentResourcesConfig resourcesConfig = AgentResourcesConfig.fromString(resources)

        assertEquals("one role is expected for ports", 1, resourcesConfig.ports.size())
        assertEquals("one role is expected for cpus", 1, resourcesConfig.cpus.size())
        assertEquals("one role is expected for mem", 1, resourcesConfig.mems.size())
        assertEquals("one role is expected for disk", 1, resourcesConfig.disks.size())

        assertEquals( "[9200-9200,9300-9300]", resourcesConfig.ports["testRole"].value)
        assertEquals( 0.2, resourcesConfig.cpus["testRole"].value, 0.0001)
        assertEquals( 256, resourcesConfig.mems["testRole"].value, 0.0001)
        assertEquals( 200, resourcesConfig.disks["testRole"].value, 0.0001)

    }

}