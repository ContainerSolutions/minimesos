package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.apache.mesos.mini.container.AbstractContainer;

import java.security.SecureRandom;

/**
 * Test container
 */
class HelloWorldContainer extends AbstractContainer {

    public static final String HELLO_WORLD_IMAGE = "tutum/hello-world";
    public static final int PORT = 80;

    protected HelloWorldContainer(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    protected void pullImage() {
        dockerUtil.pullImage(HELLO_WORLD_IMAGE, "latest");
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withName("hello-world_" + new SecureRandom().nextInt());
    }
}
