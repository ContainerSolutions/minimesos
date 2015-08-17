package org.apache.mesos.mini;

import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.util.JsonContainerSpec;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class JsonContainerSpecTest {
    private static final MesosClusterConfig config = MesosClusterConfig.builder()
            .numberOfSlaves(3)
            .privateRegistryPort(16000) // Currently you have to choose an available port by yourself
            .proxyPort(8777)
            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
            .build();
    @ClassRule
    public static MesosCluster cluster = new MesosCluster(config);

    public static String jsonSpec = "{" +
            "  \"containers\": [" +
            "    {" +
            "      \"image\": \"tutum/hello-world\"," +
            "      \"tag\": \"latest\"," +
            "      \"with\": {" +
            "        \"name\": \"hello-world_1\"," +
            "        \"exposed-ports\": [80]" +
            "      }" +
            "    }," +
            "    {" +
            "      \"image\": \"tutum/hello-world\"," +
            "      \"tag\": \"latest\"," +
            "      \"with\": {" +
            "        \"name\": \"hello-world_2\"," +
            "        \"exposed-ports\": [8080]" +
            "      }" +
            "    }" +
            "  ]" +
            "}";

    public static String jsonSpecNoImage = "{" +
            "  \"containers\": [" +
            "    {" +
            "      \"tag\": \"latest\"," +
            "      \"with\": {" +
            "        \"name\": \"hello-world\"," +
            "        \"exposed-ports\": [80]" +
            "      }" +
            "    }" +
            "  ]" +
            "}";

    public static String jsonSpecNoName = "{" +
            "  \"containers\": [" +
            "    {" +
            "      \"image\": \"tutum/hello-world\"," +
            "      \"tag\": \"latest\"," +
            "      \"with\": {" +
            "        \"exposed-ports\": [80]" +
            "      }" +
            "    }" +
            "  ]" +
            "}";

    public static List<AbstractContainer> containersList;

    @BeforeClass
    public static void provideSpec() throws Exception {
        JsonContainerSpec jsonSpecInstance = new JsonContainerSpec(jsonSpec, config.dockerClient);
        containersList = jsonSpecInstance.getContainers();

    }

    @Test
    public void testSpecParsed() {
        assertTrue(this.containersList.size() == 2);
    }

    @Test
    public void ensureAbstractContainerInstance () {
        assertThat(this.containersList.get(0), instanceOf(AbstractContainer.class));
    }

    @Test(expected=Exception.class)
    public void ensureNoImageThrowsException() throws Exception {
        JsonContainerSpec jsonSpecInstance = new JsonContainerSpec(jsonSpecNoImage, config.dockerClient);
        containersList = jsonSpecInstance.getContainers();
    }
    @Test(expected=Exception.class)
    public void ensureNoNameThrowsException() throws Exception {
        JsonContainerSpec jsonSpecInstance = new JsonContainerSpec(jsonSpecNoName, config.dockerClient);
        containersList = jsonSpecInstance.getContainers();
    }
    @Test
    public void ensureSpecContainersExecute() {
        for (AbstractContainer ac : containersList) {
            cluster.addAndStartContainer(ac);
        }

    }
}
