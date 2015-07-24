package org.apache.mesos.mini.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Container;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.docker.ResponseCollector;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Extend this class to start and manage your own containers
 */
public abstract class AbstractContainer {

    private static Logger LOGGER = Logger.getLogger(AbstractContainer.class);

    protected final DockerClient dockerClient;

    private String containerId = "";

    protected AbstractContainer(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    /**
     * Implement this method to pull your image. This will be called before the container is run.
     * For example, see {@link org.apache.mesos.mini.docker.DockerProxy}
     */
    protected abstract void pullImage();

    /**
     * Implement this method to create your container.
     *
     * For example, see {@link org.apache.mesos.mini.docker.DockerProxy}
     *
     * @return Your {@link CreateContainerCmd} for docker.
     */
    protected abstract CreateContainerCmd dockerCommand();

    /**
     * Starts the container and waits until is started
     */
    public void start() {
        pullImage();

        CreateContainerCmd createCommand = dockerCommand();
        LOGGER.debug("Creating container [" + createCommand.getName() + "]");
        containerId = createCommand.exec().getId();

        dockerClient.startContainerCmd(containerId).exec();

        await().atMost(20, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(new ContainerIsRunning<Boolean>(containerId));

        LOGGER.debug("Container is up and running");
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

    protected void pullImage(String imageName, String registryTag) {
        LOGGER.debug("Pulling image [" + imageName + ":" + registryTag + "]");
        InputStream responsePullImages = dockerClient.pullImageCmd(imageName).withTag(registryTag).exec();
        ResponseCollector.collectResponse(responsePullImages);
    }

    private class ContainerIsRunning<T> implements Callable<Boolean> {

        private String containerId;

        public ContainerIsRunning(String containerId) {
            this.containerId = containerId;
        }

        @Override
        public Boolean call() throws Exception {
            List<Container> containers = dockerClient.listContainersCmd().exec();
            for (Container container : containers) {
                if (container.getId().equals(containerId)) {
                    return true;
                }
            }
            return false;
        }
    }
}
