package org.apache.mesos.mini.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.mesos.MesosClusterConfig;

import java.security.SecureRandom;

public class PrivateDockerRegistry extends AbstractContainer {

    private final MesosClusterConfig config;
    private final String REGISTRY_IMAGE_NAME = "registry";
    private final String REGISTRY_TAG = "2.0.1";

    public PrivateDockerRegistry(DockerClient dockerClient, MesosClusterConfig config) {
        super(dockerClient);
        this.config = config;
    }

    String generateRegistryContainerName() {
        return "registry_" + new SecureRandom().nextInt();
    }

    @Override
    protected void pullImage() {
        pullImage(REGISTRY_IMAGE_NAME, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(REGISTRY_IMAGE_NAME + ":" + REGISTRY_TAG)
                .withName(generateRegistryContainerName())
                .withExposedPorts(ExposedPort.parse("5000"))
                .withEnv("REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY=/var/lib/registry")
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.privateRegistryPort + ":5000"));
    }
}