package com.containersol.minimesos.main;

import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.ClusterContainers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


public class CommandUpTest {

    @Test
    public void testDefaultClusterConfig() throws IOException {

        CommandUp commandUp = new CommandUp();

        ClusterArchitecture architecture = commandUp.getClusterArchitecture();
        assertNotNull("architecture is not loaded", architecture);

        ClusterContainers clusterContainers = architecture.getClusterContainers();
        assertNotNull("cluster containers are not loaded", clusterContainers);

        assertTrue("zooker is required component of cluster", clusterContainers.isPresent(ClusterContainers.Filter.zooKeeper()));
        assertTrue("Mesos Master is required component of cluster", clusterContainers.isPresent(ClusterContainers.Filter.mesosMaster()));

    }

    @Test
    public void testBasicClusterConfig() throws IOException {

        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/test/resources/clusterconfig/basic.groovy");

        ClusterArchitecture architecture = commandUp.getClusterArchitecture();
        assertNotNull("architecture is not loaded", architecture);

        ClusterContainers clusterContainers = architecture.getClusterContainers();
        assertNotNull("cluster containers are not loaded", clusterContainers);

        assertTrue("zooker is required component of cluster", clusterContainers.isPresent(ClusterContainers.Filter.zooKeeper()));
        assertTrue("Mesos Master is required component of cluster", clusterContainers.isPresent(ClusterContainers.Filter.mesosMaster()));

    }


}