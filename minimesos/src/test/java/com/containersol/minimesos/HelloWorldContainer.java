package com.containersol.minimesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.containersol.minimesos.container.AbstractContainer;

/**
 * Test container
 */
class HelloWorldContainer extends AbstractContainer {

    public static final String HELLO_WORLD_IMAGE = "tutum/hello-world";
    public static final int PORT = 80;
    public static final String CONTAINER_NAME_PATTERN = "^helloworld-[0-9a-f\\-]*$";

    protected HelloWorldContainer(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    protected String getRole() {
        return "helloworld";
    }

    @Override
    protected void pullImage() {
        pullImage(HELLO_WORLD_IMAGE, "latest");
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withPrivileged(true)
                .withName( buildContainerName() );
    }

    @Override
    public String buildContainerName() {
        return "helloworld-" + getUuid();
    }

}
