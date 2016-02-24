package com.containersol.minimesos.config;

import org.junit.Test;

import static org.junit.Assert.*;

public class AgentResourcesTest {

    @Test
    public void testDefaultResourcesAsString() {

        AgentResources resources = new AgentResources()
        String asString = resources.asMesosString()

        String expected = String.format("ports(*):%s; cpus(*):%s; mem(*):%s; disk(*):%s",
                AgentResources.DEFAULT_PORTS.value,
                AgentResources.DEFAULT_CPU.value,
                AgentResources.DEFAULT_MEM.value,
                AgentResources.DEFAULT_DISK.value)

        assertEquals(expected, asString)

    }

    @Test
    public void testPortsFromString() {

        String strResources = "ports(*):[8081-8082]"
        AgentResources resources = AgentResources.fromString(strResources);

        assertEquals(0, resources.cpus.size())
        assertEquals(0, resources.disks.size())
        assertEquals(0, resources.mems.size())

        assertEquals(1, resources.ports.size())
        assertEquals("[8081-8082]", String.valueOf(resources.ports["*"].value))

    }

    @Test
    public void testPortsCpusFromString() {

        String strResources = "ports(*):[8081-8082]; cpus(*):1.2"
        AgentResources resources = AgentResources.fromString(strResources);

        assertEquals(1, resources.cpus.size())
        double actual = resources.cpus["*"].value
        assertEquals(1.2, actual, 0.001 )

        assertEquals(0, resources.disks.size())
        assertEquals(0, resources.mems.size())

        assertEquals(1, resources.ports.size())
        assertEquals("[8081-8082]", resources.ports["*"].value)

    }

}