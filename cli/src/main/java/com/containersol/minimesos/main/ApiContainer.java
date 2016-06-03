package com.containersol.minimesos.main;

import com.containersol.minimesos.config.ContainerConfigBlock;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;
import static java.lang.String.valueOf;

public class ApiContainer extends AbstractContainer {

    public ApiContainer() {
        super(new ContainerConfigBlock("containersol/minimesos-api", "latest"));
    }

    @Override protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(0);
        return DockerClientFactory.build().createContainerCmd(getImageName() + ":" + getImageTag())
            .withEnv(newEnvironment()
                .withValue("PORT", valueOf(exposedPort.getPort()))
                .createEnvironment())
            .withPrivileged(true)
            .withName(getName())
            .withExposedPorts(exposedPort);
    }

    @Override public String getRole() {
        return "api";
    }
}
