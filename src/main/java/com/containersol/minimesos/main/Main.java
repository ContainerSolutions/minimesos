package com.containersol.minimesos.main;

import com.containersol.minimesos.MesosCluster;
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

    public static void main(String[] args)  {
        if (args.length == 0) {
            printIpOrUsage();
        } else if (args.length == 1) {
            if (args[0].equals("up")) {
                doUp();
            } else if (args[0].equals("destroy")) {
                MesosCluster.destroy();
            }
        }
    }

    private static void doUp() {
        String clusterId = MesosCluster.readClusterId();
        if (clusterId != null) {
            MesosCluster.printMasterIp(clusterId);
        } else {
            MesosCluster cluster = new MesosCluster(
                    MesosClusterConfig.builder()
                            .numberOfSlaves(1)
                            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]"})
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

    private static void printIpOrUsage() {
        String clusterId = MesosCluster.readClusterId();
        if (clusterId == null) {
            MesosCluster.printUsage();
        } else {
            MesosCluster.printMasterIp(clusterId);
        }
    }
}
