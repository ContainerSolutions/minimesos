package com.containersol.minimesos.cmdhooks.up;

import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.cmdhooks.HookExecutor;
import com.containersol.minimesos.main.CommandInterface;
import com.containersol.minimesos.mesos.ZooKeeper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * StartMesosConsul manages mesos-consul start-up in case --consul flag was passed
 */
public class StartMesosConsul extends HookExecutor {
    Logger LOGGER = LoggerFactory.getLogger(HookExecutor.class);
    protected CommandInterface cmd;

    public StartMesosConsul setCmd(CommandInterface cmd) {
        this.cmd = cmd;
        return this;
    }
    @Override
    public Object call() throws Exception {
        if (!cmd.getStartConsul()) {
            return null;
        }
        String mesosConsul = "";
        try {
            mesosConsul = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("marathon/mesos-consul.json"), "UTF-8");
            MesosCluster.executeMarathonTask(clusterId,
                    mesosConsul
                            .replace("{{MINIMESOS_ZOOKEEPER}}", ZooKeeper.formatZKAddress(MesosCluster.getContainerIp(clusterId, "zookeeper")) + "/mesos")
                            .replace("{{MINIMESOS_CONSUL_IP}}", MesosCluster.getContainerIp(clusterId, "consul")));
        } catch (Exception e) {
            LOGGER.error("Can't start marathon task: " + e.getMessage());
        }
        return null;
    }
}
