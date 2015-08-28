package org.apache.mesos.mini.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import org.apache.log4j.Logger;

import java.io.InputStream;

public class ImagePusher {
    private static final Logger LOGGER = Logger.getLogger(ImagePusher.class.getCanonicalName());
    private final DockerClient dockerClient;
    private final String privateRepoURL;
    private final String mesosClusterContainerId;

    public ImagePusher(DockerClient dockerClient, String privateRepoURL, String mesosClusterContainerId) {
        this.dockerClient = dockerClient;
        this.privateRepoURL = privateRepoURL;
        this.mesosClusterContainerId = mesosClusterContainerId;
    }

    /**
     * Injects the latest version of the image.
     */
    public void injectImage(String imageName) {
        injectImage(imageName, "latest");
    }
    public void injectImage(String imageName, String tag) {
        String imageNameWithTag = imageName + ":" + tag;
        LOGGER.info("Injecting image [" + privateRepoURL + "/" + imageNameWithTag + "]");

        // Retag image in local docker daemon
        dockerClient.tagImageCmd(imageName, privateRepoURL + "/" + imageName, tag).withForce().exec();

        // Push from local docker daemon to private registry
        InputStream responsePushImage = dockerClient.pushImageCmd(privateRepoURL + "/" + imageName).withTag(tag).exec();
        String fullLog = ResponseCollector.collectResponse(responsePushImage);
        if (!successfulPush(fullLog)){
            throw new DockerException("Unable to push image: " + imageNameWithTag + "\n" + fullLog, 404);
        }

        // As mesos-local daemon, pull from private registry
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(mesosClusterContainerId).withAttachStdout(true).withCmd("docker", "pull", "private-registry:5000/" + imageNameWithTag).exec();
        InputStream execCmdStream = dockerClient.execStartCmd(exec.getId()).exec();
        fullLog = ResponseCollector.collectResponse(execCmdStream);
        if (!successfulPull(fullLog)){
            throw new DockerException("Unable to pull image: " + imageNameWithTag + "\n" + fullLog, 404);
        }

        // As mesos-local daemon, retag in local registry
        exec = dockerClient.execCreateCmd(mesosClusterContainerId).withAttachStdout(true).withCmd("docker", "tag", "-f", "private-registry:5000/" + imageNameWithTag, imageNameWithTag).exec();
        dockerClient.execStartCmd(exec.getId()).exec(); // This doesn't produce any log messages
        LOGGER.info("Succesfully injected [" + privateRepoURL + "/" + imageNameWithTag + "]");
    }

    private boolean successfulPull(String fullLog) {
        return fullLog.contains("up to date") || fullLog.contains("Downloaded newer image");
    }

    private boolean successfulPush(String fullLog) {
        return fullLog.contains("successfully pushed") || fullLog.contains("already pushed") || fullLog.contains("already exists");
    }
}