package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Container;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.docker.PrivateDockerRegistry;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosContainer;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test inner docker client.
 * Should instantiate container INSIDE the daemon of the MesosContainer (i.e. not directly visible)
 * The only way to do this on a mac is via a proxy.
 * At the end, the internal containers should be removed, and all local containers should be deleted.
 */
public class InnerDockerTest {
    @Test
    public void shouldStart() {
        MesosClusterConfig config = MesosClusterConfig.builder().defaultDockerClient()
                .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
                .build();
        PrivateDockerRegistry registry = new PrivateDockerRegistry(config.dockerClient, config);
        registry.start();
        MesosContainer mesosContainer = new MesosContainer(config.dockerClient, config, registry.getContainerId());
        mesosContainer.start();

        HelloWorldNoRemoveContainer helloWorldContainer = new HelloWorldNoRemoveContainer(mesosContainer.getInnerDockerClient());
        helloWorldContainer.start();

        List<Container> containers = mesosContainer.getInnerDockerClient().listContainersCmd().exec();
        assertEquals(1, containers.size());

        mesosContainer.remove(); // I.e. we don't remove the hello world container, mini-mesos should do this automatically.
        registry.remove();
        containers = config.dockerClient.listContainersCmd().exec();
        assertEquals(0, containers.size());
    }



    class HelloWorldNoRemoveContainer extends AbstractContainer {

        public static final String HELLO_WORLD_IMAGE = "tutum/hello-world";

        protected HelloWorldNoRemoveContainer(DockerClient dockerClient) {
            super(dockerClient);
        }

        // Override remove method to do nothing. We are force removing in the mesosContainer.
        @Override
        public void remove() {
        }

        @Override
        protected void pullImage() {
            pullImage(HELLO_WORLD_IMAGE, "latest");
        }

        @Override
        protected CreateContainerCmd dockerCommand() {
            return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withName("hello-world_" + new SecureRandom().nextInt());
        }


    }
}
