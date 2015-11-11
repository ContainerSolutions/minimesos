package com.containersol.minimesos.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.log4j.Logger;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Extend this class to start and manage your own containers
 */
public abstract class AbstractContainer {

    private static Logger LOGGER = Logger.getLogger(AbstractContainer.class);

    private final String randomId;

    private String containerId = "";

    private boolean removed;

    protected DockerClient dockerClient;

    protected AbstractContainer(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        this.randomId = Integer.toUnsignedString(new SecureRandom().nextInt());
    }

    /**
     * Implement this method to pull your image. This will be called before the container is run.
     */
    protected abstract void pullImage();

    /**
     * Implement this method to create your container.
     *
     * @return Your {@link CreateContainerCmd} for docker.
     */
    protected abstract CreateContainerCmd dockerCommand();

    /**
     * Starts the container and waits until is started
     */
    public void start(int timeout) {
        pullImage();

        CreateContainerCmd createCommand = dockerCommand();
        LOGGER.debug("Creating container [" + createCommand.getName() + "]");
        containerId = createCommand.exec().getId();

        dockerClient.startContainerCmd(containerId).exec();

        try {
            await().atMost(timeout, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(new ContainerIsRunning<Boolean>(containerId));
        } catch (ConditionTimeoutException cte) {
            LOGGER.error(String.format("Container did not start within %d seconds", timeout));
//            InputStream logs = dockerClient.logContainerCmd(containerId).exec();

        }

        LOGGER.debug("Container is up and running");
    }

    /**
     * @return the hostname of the container
     */
    public String getHostname() {
        String res = "";
        if (!getContainerId().isEmpty()) {
            res = dockerClient.inspectContainerCmd(containerId).exec().getConfig().getHostName();
        }
        return res;
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
        try {
            dockerClient.removeContainerCmd(containerId).withForce().withRemoveVolumes(true).exec();
            this.removed = true;
        } catch (Exception e) {
            LOGGER.error("Could not remove container " + dockerCommand().getName(), e);
        }
    }

    protected Boolean imageExists(String imageName, String registryTag) {
        List<Image> images = dockerClient.listImagesCmd().exec();
        for (Image image : images) {
            for (String repoTag : image.getRepoTags()) {
                if (repoTag.equals(imageName + ":" + registryTag)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void pullImage(String imageName, String registryTag) {
        if (imageExists(imageName, registryTag)) {
            return;
        }

        LOGGER.info("Image [" + imageName + ":" + registryTag + "] not found. Pulling...");

        PullImageResultCallback callback = new PullImageResultCallback() {
            @Override
            public void awaitSuccess() {
                LOGGER.info("Finished pulling the image: " + imageName + ":" + registryTag);
            }
            @Override
            public void onNext(PullResponseItem item) {
                if (item.getStatus() == null) {
                    LOGGER.info("Error pulling image or image not found in registry: " + imageName + ":" + registryTag);
                    System.exit(1);
                }
                if (!item.getStatus().contains("Downloading") &&
                        !item.getStatus().contains("Extracting")) {
                    LOGGER.debug("Status: " + item.getStatus());
                }
            }
        };

        dockerClient.pullImageCmd(imageName).withTag(registryTag).exec(callback);
        await().atMost(Duration.FIVE_MINUTES).until(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return imageExists(imageName, registryTag);
            }
        });
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

    @Override public String toString() {
        return String.format("Container: %s", this.getContainerId());
    }

    public boolean isRemoved() {
        return removed;
    }

    public String getRandomId() {
        return randomId;
    }
}
