package com.containersol.minimesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.containersol.minimesos.container.AbstractContainer;

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
        pullImage(HELLO_WORLD_IMAGE, "latest");
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withPrivileged(true).withName("hello-world_" + new SecureRandom().nextInt());
    }
}
