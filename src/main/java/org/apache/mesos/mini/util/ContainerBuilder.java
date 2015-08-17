package org.apache.mesos.mini.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.mesos.mini.container.AbstractContainer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aleks on 17/08/15.
 */
class ContainerBuilder extends AbstractContainer {
    protected JsonContainerSpec.ContainerSpecInner providedSpec;

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
        List<ExposedPort> expPort = new ArrayList<>();

        for (String port : providedSpec.with.exposed_ports) {
            expPort.add(ExposedPort.parse(port));
        }
        if (providedSpec.with.exposed_ports.size() > 0) {
            cmd.withExposedPorts(expPort.toArray(new ExposedPort[expPort.size()]));
        }

        return cmd;
    }

}
