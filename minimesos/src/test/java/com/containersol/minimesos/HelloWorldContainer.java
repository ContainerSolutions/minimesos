package com.containersol.minimesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.containersol.minimesos.container.AbstractContainer;

/**
 * Test container
 */
class HelloWorldContainer extends AbstractContainer {

    public static final String HELLO_WORLD_IMAGE = "tutum/hello-world";
    public static final String CONTAINER_NAME_PATTERN = "^helloworld-[0-9a-f\\-]*$";

    protected HelloWorldContainer(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    public String getRole() {
        return "helloworld";
    }

    @Override
    protected void pullImage() {
        pullImage(HELLO_WORLD_IMAGE, "latest");
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withPrivileged(true)
                .withName( getName() );
    }

    @Override
    public String getName() {
        return "helloworld-" + getUuid();
    }

}
