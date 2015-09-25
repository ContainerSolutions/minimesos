package org.apache.mesos.mini.mesos;

import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.util.ContainerBuilder;
import org.apache.mesos.mini.util.JsonContainerSpec;

import java.util.List;

;

/**
 * Created by aleks on 18/08/15.
 */
public class Main {
    public static Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String args[]) throws Exception {
        MesosClusterConfig config = MesosClusterConfig.builder()
                .numberOfSlaves(3)
                .privateRegistryPort(16000) // Currently you have to choose an available port by yourself
                .proxyPort(8777)
                .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
                .build();
        MesosCluster cluster = new MesosCluster(config);
        cluster.start();
        String marathonJson = String.format("{" +
                "  \"containers\": [" +
                "    {" +
                "      \"image\": \"mesosphere/marathon\"," +
                "      \"tag\": \"latest\"," +
                "      \"with\": {" +
                "        \"name\": \"marathon\"," +
                "        \"expose\": [8080]," +
                "        \"environment\": [\"MARATHON_MASTER=%1$s\", \"MARATHON_ZK=%1$s\"]" +
//                "        \"links\": [\"%s:%s\"]" +
                "      }" +
                "    }" +
                "  ]" +
                "}"
                , String.format("zk://%s:2181" + "/mesos", cluster.getMesosContainer().getIpAddress()));

        try {
            List<ContainerBuilder> al = new JsonContainerSpec(marathonJson, config.dockerClient).getContainers();
            for(ContainerBuilder o : al) {
//                cluster.injectImage(o.getProvidedSpec().image, o.getProvidedSpec().tag);
                cluster.addAndStartContainer(o);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.info("Cluster started. Press CTRL+C to exit");
        while (true) {
            Thread.sleep(1000);
        }
    }
}
