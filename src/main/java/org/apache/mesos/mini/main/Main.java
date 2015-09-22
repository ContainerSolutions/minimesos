package org.apache.mesos.mini.main;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import sun.misc.Signal;

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
                        .privateRegistryPort(15000) // Currently you have to choose an available port by yourself
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
