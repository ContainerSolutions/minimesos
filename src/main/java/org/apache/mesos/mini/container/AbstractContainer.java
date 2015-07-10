package org.apache.mesos.mini.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.apache.mesos.mini.docker.DockerUtil;

/**
 * Extend this class to start and manage your own containers
 */
public abstract class AbstractContainer {
    protected final DockerClient dockerClient;
    protected final DockerUtil dockerUtil;
    private String containerId = "";

    protected AbstractContainer(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        dockerUtil = new DockerUtil(this.dockerClient);
    }

    protected abstract void pullImage();

    protected abstract CreateContainerCmd dockerCommand();

    public void start() {
        pullImage();
        containerId = dockerUtil.createAndStart(dockerCommand());
    }

    public String getContainerId() {
        return containerId;
    }

    public String getIpAddress() {
        String res = "";
        if (!getContainerId().isEmpty()) {
            res = dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
        }
        return res;
    }
}
