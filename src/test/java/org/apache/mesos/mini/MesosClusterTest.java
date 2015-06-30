package org.apache.mesos.mini;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MesosClusterTest {

    private MesosCluster cluster;

    @Before
    public void startMesosCluster() {
        this.cluster = new MesosCluster(3);
        cluster.start();
    }


    @Test
    public void mesosClusterCanBeStarted() {
        Assert.assertNotNull(cluster.startContainerCmd.getContainerId());
    }


    @After
    public void stopMesosCluster() {
        cluster.stop();
    }

}
