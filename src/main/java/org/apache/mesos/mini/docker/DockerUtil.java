package org.apache.mesos.mini.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.util.ContainerEchoResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Utility for Docker related tasks such as pulling images and reading output.
 */
public class DockerUtil {

    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);
    private final DockerClient dockerClient;

    public DockerUtil(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public static String consumeInputStream(InputStream response) {

        StringWriter logwriter = new StringWriter();

        try {
            LineIterator itr = IOUtils.lineIterator(response, "UTF-8");

            while (itr.hasNext()) {
                String line = itr.next();
                logwriter.write(line + (itr.hasNext() ? "\n" : ""));
                LOGGER.info(line);
            }
            response.close();

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    public String createAndStart(CreateContainerCmd createCommand) {
        LOGGER.debug("*****************************         Creating container \"" + createCommand.getName() + "\"         *****************************");

        CreateContainerResponse r = createCommand.exec(); // we assume that if exec fails no container with that name is created so we don't need to clean up
        String containerId = r.getId();

        StartContainerCmd startMesosClusterContainerCmd = dockerClient.startContainerCmd(containerId);
        startMesosClusterContainerCmd.exec();

        awaitEchoResponse(containerId, createCommand.getName());

        return containerId;
    }

    public void awaitEchoResponse(String containerId, String containerName) throws ConditionTimeoutException {
        await("Waiting for container: " + containerName)
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(new ContainerEchoResponse(dockerClient, containerId), is(true));

    }

    public void pullImage(String imageName, String registryTag) {
        LOGGER.debug("*****************************         Pulling image \"" + imageName + ":" + registryTag + "\"         *****************************");

        InputStream responsePullImages = dockerClient.pullImageCmd(imageName).withTag(registryTag).exec();
        String fullLog = DockerUtil.consumeInputStream(responsePullImages);
        assertThat(fullLog, anyOf(containsString("Download complete"), containsString("Already exists")));
    }

}