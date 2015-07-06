package org.apache.mesos.mini;

import org.json.JSONObject;
import org.junit.*;

public class MesosClusterTest {

    @Rule
    public MesosCluster cluster = new MesosCluster(3, "ports(*):[9200-9299,9300-9399]");




    @Test
    public void mesosClusterCanBeStarted () throws Exception{

        JSONObject stateInfo = cluster.getStateInfo();

        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
    }

}
