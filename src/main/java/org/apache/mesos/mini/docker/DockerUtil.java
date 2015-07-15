package org.apache.mesos.mini.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.util.ContainerEchoResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Utility for Docker related tasks such as pulling images and reading output.
 */
public class DockerUtil {

    private static final ArrayList<String> containerIds = new ArrayList<String>(); // Massive hack. Todo. Static so it shuts down all containers.
    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);
    private final DockerClient dockerClient;

    public DockerUtil(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("Running shutdown hook");
                DockerUtil.this.stop();
            }
        });
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

    public String getContainerIp(String containerId) {

        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();

        assertThat(inspectContainerResponse.getNetworkSettings().getIpAddress(), notNullValue());
        return inspectContainerResponse.getNetworkSettings().getIpAddress();
    }

    public void buildImageFromFolder(File dir, String tag) {
        String fullLog;
        assert dir.isDirectory();
        InputStream responseBuildImage = dockerClient.buildImageCmd(dir).withTag(tag).exec();

        fullLog = consumeInputStream(responseBuildImage);
        assertThat(fullLog, containsString("Successfully built"));
    }

    public void buildImageFromFolder(String name, String tag) {
        File folder = new File(Thread.currentThread().getContextClassLoader().getResource(name).getFile());
        buildImageFromFolder(folder, tag);
    }

    public String createAndStart(CreateContainerCmd createCommand) {
        LOGGER.debug("*****************************         Creating container \"" + createCommand.getName() + "\"         *****************************");

        CreateContainerResponse r = createCommand.exec(); // we assume that if exec fails no container with that name is created so we don't need to clean up
        String containerId = r.getId();

        if (createCommand.getName() != null){
            containerIds.add(createCommand.getName()); // for better readability when logging the cleanup/removal of the containers
        } else {
            containerIds.add(containerId);
        }

        StartContainerCmd startMesosClusterContainerCmd = dockerClient.startContainerCmd(containerId);
        startMesosClusterContainerCmd.exec();


//        awaitEchoResponse(containerId, createCommand.getName());

        return containerId;
    }

    public void awaitEchoResponse(String containerId) throws ConditionTimeoutException {
      awaitEchoResponse(containerId, containerId);
    }

    public void awaitEchoResponse(String containerId, String containerName) throws ConditionTimeoutException {
        await("Waiting for container: " + containerName)
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(new ContainerEchoResponse(dockerClient, containerId), is(true));

    }

    public void pullImage(String imageName, String registryTag) {
        LOGGER.debug("*****************************         Pulling image \"" + imageName + ":"+registryTag+"\"         *****************************");

        InputStream responsePullImages = dockerClient.pullImageCmd(imageName).withTag(registryTag).exec();
        String fullLog = DockerUtil.consumeInputStream(responsePullImages);
        assertThat(fullLog, anyOf(containsString("Download complete"), containsString("Already exists")));
    }

    public void stop() {
        for (String containerId : containerIds) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce().exec();
                LOGGER.info("*****************************         Removing container \"" + containerId + "\"         *****************************");
            } catch (Exception ignore){}
        }
        containerIds.clear();
    }
}
