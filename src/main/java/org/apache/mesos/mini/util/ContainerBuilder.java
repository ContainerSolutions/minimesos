package org.apache.mesos.mini.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
        LOGGER.info(String.format("Creating container %s cmd", providedSpec.with.name));

        if (providedSpec.with.expose != null) {
            List<ExposedPort> ep = new ArrayList<>();
            for (Integer o : providedSpec.with.expose) { ep.add(new ExposedPort(o)); }
            LOGGER.info(providedSpec.with.expose);
            cmd.withExposedPorts(ep.toArray(new ExposedPort[ep.size()]));
        }
        if (providedSpec.with.cmd != null) {
            cmd.withCmd(providedSpec.with.cmd.toArray(new String[providedSpec.with.cmd.size()]));
        }
        if (providedSpec.with.volumes != null) {
            List<Volume> vl = new ArrayList<>();
            for (String o : providedSpec.with.volumes) { vl.add(new Volume(o)); }
            LOGGER.info(providedSpec.with.volumes);
            cmd.withVolumes(vl.toArray(new Volume[vl.size()]));
        }

        if (providedSpec.with.environment != null) {
            cmd.withEnv(providedSpec.with.environment.toArray(new String[providedSpec.with.environment.size()]));
        }

        if (providedSpec.with.volumes_from != null) {
            List<VolumesFrom> vlf = new ArrayList<>();
            for (String o : providedSpec.with.volumes_from) { vlf.add(new VolumesFrom(o)); }
            LOGGER.info(providedSpec.with.volumes_from);
            cmd.withVolumesFrom(vlf.toArray(new VolumesFrom[vlf.size()]));
        }

        if (providedSpec.with.links != null) {
            List<Link> ll = new ArrayList<>();
            for (String o : providedSpec.with.links) { ll.add(Link.parse(o)); }
            LOGGER.info(providedSpec.with.links);
            cmd.withLinks(ll.toArray(new Link[ll.size()]));
        }

        return cmd;
    }

    public JsonContainerSpec.ContainerSpecInner getProvidedSpec() {
        return providedSpec;
    }

}