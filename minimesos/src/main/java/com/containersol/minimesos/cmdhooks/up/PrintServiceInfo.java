package com.containersol.minimesos.cmdhooks.up;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.cmdhooks.CliCommandHookExecutor;

/**
 * Created by alg on 1/18/16.
 */
public class PrintServiceInfo extends CliCommandHookExecutor {

    @Override
    public Object call() throws Exception {
        MesosCluster.printServiceUrl(clusterId, "master", cmd);
        MesosCluster.printServiceUrl(clusterId, "marathon", cmd);
        MesosCluster.printServiceUrl(clusterId, "zookeeper", cmd);
        if (cmd.getStartConsul()) {
            MesosCluster.printServiceUrl(clusterId, "consul", cmd);
        }
        return null;
    }
}
