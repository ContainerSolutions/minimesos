package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.util.DockerUtil;

import java.io.File;
import java.security.SecureRandom;

public class PrivateDockerRegistry {
    private final Logger LOGGER = Logger.getLogger(PrivateDockerRegistry.class);
    private final DockerClient dockerClient;
    private final MesosClusterConfig config;
    private final DockerUtil dockerUtil;
    private String dockerRegistryId;

    public PrivateDockerRegistry(DockerClient dockerClient, MesosClusterConfig config) {
        this.dockerClient = dockerClient;
        this.config = config;
        dockerUtil = new DockerUtil(dockerClient);
    }

    String generateRegistryContainerName() {
        return "registry_" + new SecureRandom().nextInt();
    }

    File createRegistryStorageDirectory() {
        File registryStorageRootDir = new File(".registry");

        if (!registryStorageRootDir.exists()) {
            LOGGER.info("The private registry storage root directory doesn't exist, creating one...");
            registryStorageRootDir.mkdir();
        }
        return registryStorageRootDir;
    }

    public void startPrivateRegistryContainer() {
        final String REGISTRY_IMAGE_NAME = "registry";
        final String REGISTRY_TAG = "0.9.1";

        dockerUtil.pullImage(REGISTRY_IMAGE_NAME, REGISTRY_TAG);

        CreateContainerCmd command = dockerClient.createContainerCmd(REGISTRY_IMAGE_NAME + ":" + REGISTRY_TAG)
                .withName(generateRegistryContainerName())
                .withExposedPorts(ExposedPort.parse("5000"))
                .withEnv("STORAGE_PATH=/var/lib/registry")
                .withVolumes(new Volume("/var/lib/registry"))
                .withBinds(Bind.parse(createRegistryStorageDirectory().getAbsolutePath() + ":/var/lib/registry:rw"))
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.privateRegistryPort + ":5000"));
        this.dockerRegistryId = dockerUtil.createAndStart(command);
    }

    public String getContainerId() {
        return dockerRegistryId;
    }
}