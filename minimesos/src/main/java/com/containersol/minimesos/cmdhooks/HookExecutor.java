package com.containersol.minimesos.cmdhooks;
import com.beust.jcommander.JCommander;
import com.containersol.minimesos.cmdhooks.up.PrintServiceInfo;
import com.containersol.minimesos.cmdhooks.up.StartMesosConsul;
import com.containersol.minimesos.main.CommandUp;
import com.containersol.minimesos.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public abstract class HookExecutor implements Callable {
    protected String clusterId;
    protected CommandUp cmdUp;

    static Logger LOGGER = LoggerFactory.getLogger(HookExecutor.class);

    public HookExecutor setCmdUp(CommandUp cmdUp) {
        this.cmdUp = cmdUp;
        return this;
    }

    public HookExecutor setClusterId(String clusterId) {
        this.clusterId = clusterId;
        return this;
    }

    public static void fireCallbacks(String command, String clusterId) {
        try {
            switch (command) {
                case "up":
                    new PrintServiceInfo().setCmdUp(Main.getCommandUp()).setClusterId(clusterId).call();
                    new StartMesosConsul().setCmdUp(Main.getCommandUp()).setClusterId(clusterId).call();
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Can't start callbacks:" + e.getMessage());
        }
    }
}