package org.apache.mesos.mini.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.security.SecureRandom;

/**
 * ContainerBuilder
 */
public class ContainerBuilder extends AbstractContainer {
    protected JsonContainerSpec.ContainerSpecInner providedSpec;
    protected static Logger LOGGER = Logger.getLogger(ContainerBuilder.class);

    protected ContainerBuilder(DockerClient dockerClient, JsonContainerSpec.ContainerSpecInner providedSpec) {
        super(dockerClient);
        this.providedSpec = providedSpec;
    }

    @Override
    protected void pullImage() {
        this.pullImage(providedSpec.image, providedSpec.tag);

    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        CreateContainerCmd cmd = dockerClient.createContainerCmd(providedSpec.image).withName(providedSpec.with.name + new SecureRandom().nextInt());
        LOGGER.info(String.format("Creating container %s ContainerCmd", providedSpec.with.name));

        if (providedSpec.with.cmd != null) {
            cmd.withCmd(providedSpec.with.cmd.cmd);
        }
        if (providedSpec.with.volumes != null) {
            cmd.withVolumes(providedSpec.with.volumes.volumes);
        }

        if (providedSpec.with.environment != null) {
            cmd.withEnv(providedSpec.with.environment.env);
        }

        if (providedSpec.with.volumes_from != null) {
            cmd.withVolumesFrom(providedSpec.with.volumes_from.volumes_from);
        }

        if (providedSpec.with.links != null) {
            cmd.withLinks(providedSpec.with.links.links);
        }

        return cmd;
    }

    public JsonContainerSpec.ContainerSpecInner getProvidedSpec() {
        return providedSpec;
    }

}