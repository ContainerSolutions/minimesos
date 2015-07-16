package org.apache.mesos.mini;

import org.apache.mesos.mini.docker.DockerUtil;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.junit.ClassRule;
import org.junit.Test;

public class ImageInjectionTest {
    private static final MesosClusterConfig config = MesosClusterConfig.builder()
            .numberOfSlaves(3)
            .privateRegistryPort(15000) // Currently you have to choose an available port by yourself
            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
            .build();

    @ClassRule
    public static MesosCluster cluster = new MesosCluster(config);

    @Test
    public void testInjectImage() {
        DockerUtil dockerUtil = new DockerUtil(config.dockerClient);
        dockerUtil.pullImage("tutum/hello-world", "latest"); // Pull hello world image for test
        String url = "localhost" + ":" + config.privateRegistryPort;
        dockerUtil.injectImage(url, "tutum/hello-world", cluster.getMesosContainer().getMesosContainerID());
    }
}
