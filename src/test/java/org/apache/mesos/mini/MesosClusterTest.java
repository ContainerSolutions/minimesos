package org.apache.mesos.mini;

import org.json.JSONObject;
import org.junit.*;

public class MesosClusterTest {

    @ClassRule
    public static MesosCluster cluster = new MesosCluster(3, "ports(*):[9200-9299,9300-9399]", new String[]{"mesos/elasticsearch-executor"});


    @Test
    public void mesosClusterCanBeStarted() throws Exception {

        JSONObject stateInfo = cluster.getStateInfo();

        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
    }

    @Test
    public void mesosClusterCanBeStarted2() throws Exception {

        JSONObject stateInfo = cluster.getStateInfo();

        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
    }

}
