package org.apache.mesos.mini.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.PortBinding;
import org.apache.mesos.mini.container.AbstractContainer;

public class DockerProxy extends AbstractContainer {

    public static final String PROXY_IMAGE = "paintedfox/tinyproxy";
    public static final String PROXY_PORT = "8888";

    public DockerProxy(DockerClient dockerClient) {
        super(dockerClient);
    }

    @Override
    protected void pullImage() {
        dockerUtil.pullImage(PROXY_IMAGE, "latest");
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient.createContainerCmd(PROXY_IMAGE).withPortBindings(PortBinding.parse("0.0.0.0:" + PROXY_PORT + ":" + PROXY_PORT));
    }
}