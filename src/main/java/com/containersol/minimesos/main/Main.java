package com.containersol.minimesos.main;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterConfig;

import java.io.IOException;

/**
 *
 */
public class Main {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException, UnirestException {
        MesosCluster cluster = new MesosCluster(
                MesosClusterConfig.builder()
                        .numberOfSlaves(1)
                        .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]"})
                        .build()
        );

        cluster.start();

        // Ugly, but it seems to work. Perhaps do an actual daemon thread?
        while (true) {
            Thread.sleep(2000);
        }
    }
}
