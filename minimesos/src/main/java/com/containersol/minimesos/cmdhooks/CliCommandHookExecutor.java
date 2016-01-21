package com.containersol.minimesos.cmdhooks;
import com.containersol.minimesos.cmdhooks.up.PrintServiceInfo;
import com.containersol.minimesos.cmdhooks.up.StartMesosConsul;
import com.containersol.minimesos.main.MinimesosCliCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public abstract class CliCommandHookExecutor implements Callable {
    protected String clusterId;
    protected MinimesosCliCommand cmd;

    static Logger LOGGER = LoggerFactory.getLogger(CliCommandHookExecutor.class);

    public CliCommandHookExecutor setCmd(MinimesosCliCommand cmd) {
        this.cmd = cmd;
        return this;
    }

    public CliCommandHookExecutor setClusterId(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public static void fireCallbacks(String command, String clusterId, MinimesosCliCommand cmd) {
        try {
            switch (command) {
                case "up":
                    new PrintServiceInfo().setCmd(cmd).setClusterId(clusterId).call();
                    new StartMesosConsul().setCmd(cmd).setClusterId(clusterId).call();
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Can't start callbacks:" + e.getMessage());
        }
    }
}