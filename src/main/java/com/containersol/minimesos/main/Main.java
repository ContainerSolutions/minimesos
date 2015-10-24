package com.containersol.minimesos.main;

import com.beust.jcommander.JCommander;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.marathon.MarathonClient;
import com.containersol.minimesos.mesos.MesosClusterConfig;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main method for interacting with minimesos.
 */
public class Main {

    private static Logger LOGGER = Logger.getLogger(Main.class);

    private static CommandUp commandUp = new CommandUp();

    public static void main(String[] args)  {
        JCommander jc = new JCommander();
        jc.setProgramName("minimesos");

        CommandDestroy commandDestroy = new CommandDestroy();
        CommandHelp commandHelp = new CommandHelp();
        CommandInstall commandInstall = new CommandInstall();

        jc.addCommand("up", commandUp);
        jc.addCommand("destroy", commandDestroy);
        jc.addCommand("install", commandInstall);
        jc.addCommand("help", commandHelp);
        jc.parseWithoutValidation(args);

        if (jc.getParsedCommand() == null) {
            String clusterId = MesosCluster.readClusterId();
            if (clusterId != null) {
                MesosCluster.printMasterIp(clusterId);
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
            case "install":
                String marathonFile = commandInstall.getMarathonFile();
                String marathonIp = MesosCluster.getContainerIp(MesosCluster.readClusterId(), "marathon");
                MarathonClient.deployFramework(marathonIp, marathonFile);
                break;
            case "help":
                jc.usage();
        }
    }

    private static void doUp() {
        String clusterId = MesosCluster.readClusterId();
        if (clusterId != null) {
            MesosCluster.printMasterIp(clusterId);
        } else {
            MesosCluster cluster = new MesosCluster(
                    MesosClusterConfig.builder()
                            .slaveResources(new String[] {
                                            "ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"
                                    }
                            )
                            .mesosImageTag("0.22.1-1.0.ubuntu1404")
                            .build()
            );
            cluster.start();

            File miniMesosDir = new File(System.getProperty("user.home"), ".minimesos");
            try {
                FileUtils.forceMkdir(miniMesosDir);
                Files.write(Paths.get(miniMesosDir.getAbsolutePath() + "/minimesos.cluster"), cluster.getClusterId().getBytes());
            } catch (IOException ie) {
                LOGGER.error("Could not write .minimesos folder", ie);
                throw new RuntimeException(ie);
            }
        }
    }

}
