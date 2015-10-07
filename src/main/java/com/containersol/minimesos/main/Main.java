package com.containersol.minimesos.main;

import com.mashape.unirest.http.exceptions.UnirestException;
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

    public static void main(String[] args) throws IOException, InterruptedException, UnirestException {
        if (args.length == 0) {
            MesosCluster.print();
        } else if (args.length == 1) {
            if (args[0].equals("up")) {
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
                } catch (IOException e) {
                    LOGGER.error("Could not write .minimesos folder", e);
                    throw new RuntimeException(e);
                }

            } else if (args[0].equals("destroy")) {
                MesosCluster.destroy();
            }
        }
    }
}
