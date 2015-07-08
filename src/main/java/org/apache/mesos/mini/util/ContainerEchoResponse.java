package org.apache.mesos.mini.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import org.apache.mesos.mini.MesosCluster;

import java.io.InputStream;
import java.util.concurrent.Callable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
* Created by peldan on 08/07/15.
*/
public class ContainerEchoResponse implements Callable<Boolean> {


    private final String containerId;
    private final DockerClient dockerClient;

    public ContainerEchoResponse(DockerClient dockerClient, String containerId) {
        this.dockerClient = dockerClient;
        this.containerId = containerId;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true).withCmd("echo", "hello-container").exec();
            InputStream execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            assertThat(DockerUtil.consumeInputStream(execCmdStream), containsString("hello-container"));
        } catch (Exception e) {
            MesosCluster.LOGGER.error("An error occured while waiting for container to echo test message", e);
            return false;
        }
        return true;
    }
}
