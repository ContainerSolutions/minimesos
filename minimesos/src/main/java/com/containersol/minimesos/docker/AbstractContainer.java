package com.containersol.minimesos.docker;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ContainerConfig;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.jayway.awaitility.core.ConditionTimeoutException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Extend this class to start and manage your own containers
 */
public abstract class AbstractContainer implements ClusterProcess {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContainer.class);

    private static final int IMAGE_PULL_TIMEOUT_SECS = 30;

    private MesosCluster cluster;
    private final ContainerConfig config;
    private final String uuid;
    private String containerId;
    private String ipAddress = null;

    protected AbstractContainer(ContainerConfig config) {
        this.config = config;
        this.uuid = Integer.toUnsignedString(new SecureRandom().nextInt());
    }

    public AbstractContainer(MesosCluster cluster, String uuid, String containerId, ContainerConfig config) {
        this.cluster = cluster;
        this.uuid = uuid;
        this.containerId = containerId;
        this.config = config;
    }

    /**
     * Implement this method to pull your image. This will be called before the container is run.
     */
    public void pullImage() {
        pullImage(getImageName(), getImageTag());
    }

    /**
     * @return name of the container to use
     */
    public String getImageName() {
        return config.getImageName();
    }

    /**
     * @return verstion of the container to use
     */
    public String getImageTag() {
        return config.getImageTag();
    }

    /**
     * Implement this method to retrieveMesosCluster your container.
     *
     * @return Your {@link CreateContainerCmd} for docker.
     */
    protected abstract CreateContainerCmd dockerCommand();

    /**
     * Starts the container and waits until is started
     *
     * @param timeout in seconds
     */
    @Override
    public void start(int timeout) {
        if (containerId != null) {
            return;
        }

        pullImage();

        CreateContainerCmd createCommand = dockerCommand();
        LOGGER.debug("Creating container [" + createCommand.getName() + "]");
        containerId = createCommand.exec().getId();

        DockerClientFactory.build().startContainerCmd(containerId).exec();

        try {

            await("Container did not start within " + timeout + " seconds").atMost(timeout, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
                List<Container> containers = DockerClientFactory.build().listContainersCmd().withShowAll(true).exec();
                for (Container container : containers) {
                    if (container.getId().equals(containerId)) {
                        return true;
                    }
                }
                return false;
            });

        } catch (ConditionTimeoutException cte) {
            String errorMessage = String.format("Container [%s] did not start within %d seconds.", createCommand.getName(), timeout);
            LOGGER.error(errorMessage);
            try {
                for (String logLine : DockerContainersUtil.getDockerLogs(containerId)) {
                    LOGGER.error(logLine);
                }
            } catch (Exception e) {
                LOGGER.error("Could not print container logs", e);
            }
            throw new MinimesosException(errorMessage + " See container logs above");
        }

        LOGGER.debug(String.format("Container %s is up and running", containerId));
    }

    /**
     * @return the ID of the container.
     */
    @Override
    public String getContainerId() {
        return containerId;
    }

    @Override
    public URI getServiceUrl() {

        URI serviceUri = null;

        String protocol = getServiceProtocol();
        String host = getIpAddress();
        int port = getServicePort();
        String path = getServicePath();

        if (StringUtils.isNotEmpty(host)) {
            try {
                serviceUri = new URI(protocol, null, host, port, path, null, null);
            } catch (URISyntaxException e) {
                throw new MinimesosException("Failed to form service URL for " + getName(), e);
            }
        }

        return serviceUri;
    }

    /**
     * Enables derived classes to override
     * @return protocol of the service
     */
    protected String getServiceProtocol() {
        return "http";
    }

    /**
     * Enables derived classes to override
     * @return port of the service
     */
    protected int getServicePort() {
        return 80;
    }

    /**
     * Enables derived classes to override
     * @return protocol of the service
     */
    protected String getServicePath() {
        return "";
    }

    /**
     * @return the IP address of the container
     */
    @Override
    public String getIpAddress() {
        if (ipAddress == null) {
            retrieveIpAddress();
        }
        return ipAddress;
    }

    private synchronized void retrieveIpAddress() {
        String res = "";
        if (!getContainerId().isEmpty()) {
            res = DockerContainersUtil.getIpAddress(getContainerId());
        }
        this.ipAddress = res;
    }

    /**
     * Builds container name following the naming convention
     *
     * @return container name
     */
    @Override
    public String getName() {
        return ContainerName.get(this);
    }

    /**
     * Removes a container with force
     */
    @Override
    public void remove() {
        try {
            if (DockerContainersUtil.getContainer(containerId) != null) {
                DockerClientFactory.build().removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
            }
        } catch (Exception e) {
            LOGGER.error("Could not remove container " + dockerCommand().getName(), e);
        }
    }

    protected Boolean imageExists(String imageName, String registryTag) {
        List<Image> images = DockerClientFactory.build().listImagesCmd().exec();
        if (images.isEmpty()) {
            throw new MinimesosException("Failed to find image '" +  imageName + ":" + registryTag + ". No images found");
        }
        for (Image image : images) {
            if (image.getRepoTags() == null) {
                continue;
            }
            for (String repoTag : image.getRepoTags()) {
                if (repoTag.equals(imageName + ":" + registryTag)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void pullImage(String imageName, String registryTag) {
        LOGGER.debug("Checking if image [" + imageName + ":" + registryTag + "] exists.");

        if (imageExists(imageName, registryTag)) {
            return;
        }

        LOGGER.debug("Image [" + imageName + ":" + registryTag + "] not found. Pulling...");
        DockerContainersUtil.pullImage(imageName, registryTag, IMAGE_PULL_TIMEOUT_SECS);

        if (!imageExists(imageName, registryTag)) {
            throw new MinimesosException("Pulling of " + imageName + ":" + registryTag + " completed. However the image is not found");
        }
    }

    @Override
    public void setCluster(MesosCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public MesosCluster getCluster() {
        return cluster;
    }

    /**
     * @return if set, ID of the cluster the container belongs to
     */
    public String getClusterId() {
        return (cluster != null) ? cluster.getClusterId() : null;
    }

    @Override
    public String toString() {
        return String.format(": %s-%s-%s", getRole(), getClusterId(), uuid);
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractContainer that = (AbstractContainer) o;

        if (!StringUtils.equals(this.getClusterId(), that.getClusterId())) return false;

        if (!uuid.equals(that.uuid)) return false;
        return containerId.equals(that.containerId);
    }

    @Override
    public int hashCode() {
        int result = (cluster != null) ? cluster.hashCode() : 0;
        result = 31 * result + uuid.hashCode();
        result = 31 * result + containerId.hashCode();
        return result;
    }
}

