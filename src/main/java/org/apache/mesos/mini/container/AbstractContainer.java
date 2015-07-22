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

    /**
     * Implement this method to pull your image. This will be called before the container is run.
     * For example, see {@link org.apache.mesos.mini.docker.DockerProxy}
     */
    protected abstract void pullImage();

    /**
     * Implement this method to create your container. If you use {@link DockerUtil} then your container will be
     * automatically deleted.
     * For example, see {@link org.apache.mesos.mini.docker.DockerProxy}
     * @return Your {@link CreateContainerCmd} for docker.
     */
    protected abstract CreateContainerCmd dockerCommand();

    public void start() {
        pullImage();
        containerId = dockerUtil.createAndStart(dockerCommand());
    }

    /**
     * @return the ID of the container.
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * @return the IP address of the container
     */
    public String getIpAddress() {
        String res = "";
        if (!getContainerId().isEmpty()) {
            res = dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
        }
        return res;
    }

    public String getName() {
        return dockerCommand().getName();
    }

    /**
     * Removes a container with force
     */
    public void remove() {
        dockerClient.removeContainerCmd(containerId).withForce().exec();
    }
}
