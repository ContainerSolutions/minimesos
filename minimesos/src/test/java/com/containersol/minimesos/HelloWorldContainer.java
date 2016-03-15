package com.containersol.minimesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

/**
 * A container for testing purposes. A small web server on port 80 returns the message "hello world."
 */
class HelloWorldContainer extends AbstractContainer {

    public static final String SERVICE_NAME = "hello-world-service";
    public static final int SERVICE_PORT = 80;
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
        ExposedPort exposedPort = ExposedPort.tcp(SERVICE_PORT);
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding(SERVICE_PORT));
        return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE)
                .withEnv(String.format("SERVICE_%d_NAME=%s", SERVICE_PORT, SERVICE_NAME))
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
