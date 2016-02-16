package com.containersol.minimesos.util;

import com.containersol.minimesos.MinimesosException;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ResourceUtilTest {

    @Test(expected = MinimesosException.class)
    public void testParsePorts_emptyResourceString() {
        ResourceUtil.parsePorts("");
    }

    @Test(expected = MinimesosException.class)
    public void testParsePorts_nullResource() {
        ResourceUtil.parsePorts(null);
    }

    @Test
    public void testParsePorts_singlePortRange() {
        ArrayList<Integer> ports = ResourceUtil.parsePorts("ports(*):[8080-8080]");
        assertEquals(1, ports.size());
        assertEquals(8080, ports.get(0).intValue());
    }

    @Test
    public void testParsePorts_portRange() {
        ArrayList<Integer> ports = ResourceUtil.parsePorts("ports(*):[8080-8082]");
        assertEquals(3, ports.size());
        assertEquals(8080, ports.get(0).intValue());
        assertEquals(8081, ports.get(1).intValue());
        assertEquals(8082, ports.get(2).intValue());
    }

    @Test(expected = MinimesosException.class)
    public void testParsePorts_portRanges() {
        ResourceUtil.parsePorts("ports(*):[8080-8082],[5000-5001]");
    }

}
