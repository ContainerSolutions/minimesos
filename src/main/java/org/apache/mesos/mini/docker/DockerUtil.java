package org.apache.mesos.mini.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.util.ContainerEchoResponse;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

/**
 * Utility for Docker related tasks such as pulling images and reading output.
 */
public class DockerUtil {

    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);
    private final DockerClient dockerClient;

    public DockerUtil(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String createAndStart(CreateContainerCmd createCommand) {
        LOGGER.debug("Creating container [" + createCommand.getName() + "]");

        CreateContainerResponse r = createCommand.exec(); // we assume that if exec fails no container with that name is created so we don't need to clean up
        String containerId = r.getId();

        StartContainerCmd startMesosClusterContainerCmd = dockerClient.startContainerCmd(containerId);
        startMesosClusterContainerCmd.exec();

        awaitEchoResponse(containerId, createCommand.getName());

        return containerId;
    }

    public void awaitEchoResponse(String containerId, String containerName) throws ConditionTimeoutException {
        await("Waiting for container: " + containerName)
                .atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(new ContainerEchoResponse(dockerClient, containerId), is(true));
    }

    public void pullImage(String imageName, String registryTag) {
        LOGGER.debug("Pulling image [" + imageName + ":" + registryTag + "]");
        InputStream responsePullImages = dockerClient.pullImageCmd(imageName).withTag(registryTag).exec();
        ResponseCollector.collectResponse(responsePullImages);
    }

}