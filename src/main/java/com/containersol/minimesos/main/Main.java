package com.containersol.minimesos.main;

import com.beust.jcommander.JCommander;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main method for interacting with minimesos.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static CommandUp commandUp;

    public static void main(String[] args)  {
        JCommander jc = new JCommander();
        jc.setProgramName("minimesos");

        commandUp = new CommandUp();
        CommandDestroy commandDestroy = new CommandDestroy();
        CommandHelp commandHelp = new CommandHelp();

        jc.addCommand("up", commandUp);
        jc.addCommand("destroy", commandDestroy);
        jc.addCommand("help", commandHelp);
        jc.parseWithoutValidation(args);

        if (jc.getParsedCommand() == null) {
            String clusterId = MesosCluster.readClusterId();
            if (clusterId != null) {
                MesosCluster.printServiceUrl(clusterId, "master");
                MesosCluster.printServiceUrl(clusterId, "marathon");
            } else {
                jc.usage();
            }
            return;
        }

        switch (jc.getParsedCommand()) {
            case "up":
                doUp();
                break;
            case "destroy":
                MesosCluster.destroy();
                break;
            case "help":
                jc.usage();
        }
    }

    private static void doUp() {
        String clusterId = MesosCluster.readClusterId();
        if (clusterId == null) {
            MesosCluster cluster = new MesosCluster(
                    MesosClusterConfig.builder()
                            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]"})
                            .mesosImageTag(commandUp.getMesosImageTag())
                            .exposedHostPorts(commandUp.isExposedHostPorts())
                            .build()
            );
            cluster.start();
            File miniMesosDir = new File(System.getProperty("minimesos.dir"));
            try {
                FileUtils.forceMkdir(miniMesosDir);
                Files.write(Paths.get(miniMesosDir.getAbsolutePath() + "/minimesos.cluster"), cluster.getClusterId().getBytes());
            } catch (IOException ie) {
                LOGGER.error("Could not write .minimesos folder", ie);
                throw new RuntimeException(ie);
            }
        }
        clusterId = MesosCluster.readClusterId();
        MesosCluster.printServiceUrl(clusterId, "master");
        MesosCluster.printServiceUrl(clusterId, "marathon");
    }

}
