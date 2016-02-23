package com.containersol.minimesos.java;

import com.containersol.minimesos.mesos.ClusterArchitecture;
import org.junit.Test;

/**
 *
 */
public class MesosClusterTest {

    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder()
            .withZooKeeper()
            .withMaster()
            .withSlave("ports(*):[8080-8082]")
            .withSlave("ports(*):[8080-8082]")
            .withSlave("ports(*):[8080-8082]")
            .build();


    @Test
    public void testStart() {
        MesosCluster cluster = new MesosCluster(CONFIG);
        cluster.start();
        cluster.stop();
    }
}
