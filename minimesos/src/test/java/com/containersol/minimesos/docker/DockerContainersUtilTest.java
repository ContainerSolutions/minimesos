package com.containersol.minimesos.docker;

import org.junit.Test;

import static org.junit.Assert.*;

public class DockerContainersUtilTest {

    @Test
    public void testSize() throws Exception {

        DockerContainersUtil containers = DockerContainersUtil.getContainers(true);

        assertEquals("Containers is not looked up yet", 0, containers.size());
        assertEquals("Containers should not be found", 0, containers.filterByImage("non-existing-image-pattern").size());
        assertEquals("Containers should not be found", 0, containers.filterByName("non-existing-name-pattern").size());

    }



}
