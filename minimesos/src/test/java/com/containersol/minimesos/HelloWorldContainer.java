package com.containersol.minimesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

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
        ExposedPort exposedPort = ExposedPort.tcp(80);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding(80));
        return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE)
                .withPrivileged(true)
                .withName(getName())
                .withPortBindings(portBindings)
                .withExposedPorts(exposedPort);
    }

    @Override
    public String getName() {
        return "helloworld-" + getUuid();
    }

}
