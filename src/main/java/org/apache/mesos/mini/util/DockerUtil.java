package org.apache.mesos.mini.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.MesosClusterConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by flg on 07/07/15.
 */
public class DockerUtil {
    private final DockerClient dockerClient;
    private final MesosClusterConfig config;

    public DockerUtil(MesosClusterConfig config) {
        this.dockerClient = config.dockerClient;
        this.config = config;
    }

    public static String consumeInputStream(InputStream response) {

        StringWriter logwriter = new StringWriter();

        try {
            LineIterator itr = IOUtils.lineIterator(response, "UTF-8");

            while (itr.hasNext()) {
                String line = itr.next();
                logwriter.write(line + (itr.hasNext() ? "\n" : ""));
                MesosCluster.LOGGER.info(line);
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

    // TODO can we generalize so it takes a directory name and builds from there (so we can reuse for other docker files)
    public void buildImageFromFolder(String name) {
        String fullLog;
        InputStream responseBuildImage = dockerClient.buildImageCmd(new File(Thread.currentThread().getContextClassLoader().getResource(name).getFile())).withTag(name).exec();

        fullLog = consumeInputStream(responseBuildImage);
        assertThat(fullLog, containsString("Successfully built"));
    }
}
