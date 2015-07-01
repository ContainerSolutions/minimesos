package org.apache.mesos.mini;

import com.github.dockerjava.api.command.InspectExecCmd;
import com.github.dockerjava.api.command.InspectExecResponse;
import org.junit.*;

public class MesosClusterTest {

    @Rule
    public MesosCluster cluster = new MesosCluster(3);




    @Test
    public void mesosClusterCanBeStarted() {

//        InspectExecResponse inspectResponse = cluster.dockerClient.inspectExecCmd(cluster.startContainerCmd.getContainerId()).exec();
//        InspectExecResponse inspectResponse = cluster.dockerClient.inspectExecCmd(cluster.createContainerResponse.getId()).exec();





        Assert.assertNotNull(cluster.startContainerCmd.getContainerId());
    }

}
