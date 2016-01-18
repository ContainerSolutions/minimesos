package com.containersol.minimesos.cmdhooks.up;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.cmdhooks.HookExecutor;
import com.containersol.minimesos.main.CommandUp;

/**
 * Created by alg on 1/18/16.
 */
public class PrintServiceInfo extends HookExecutor {

    @Override
    public Object call() throws Exception {
        MesosCluster.printServiceUrl(clusterId, "master", cmdUp);
        MesosCluster.printServiceUrl(clusterId, "marathon", cmdUp);
        MesosCluster.printServiceUrl(clusterId, "zookeeper", cmdUp);
        if (cmdUp.getStartConsul()) {
            MesosCluster.printServiceUrl(clusterId, "consul", cmdUp);
        }
        return null;
    }
}
