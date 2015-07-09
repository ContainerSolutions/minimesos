package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import org.apache.mesos.mini.util.DockerUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import java.io.File;
import java.io.InputStream;
import java.security.SecureRandom;

public class PrivateDockerRegistry {
    private final DockerClient dockerClient;
    private final MesosClusterConfig config;
    private final DockerUtil dockerUtil;

    public PrivateDockerRegistry(DockerClient dockerClient, MesosClusterConfig config) {
        this.dockerClient = dockerClient;
        this.config = config;
        dockerUtil = new DockerUtil(dockerClient);
    }

    void pullDindImagesAndRetagWithoutRepoAndLatestTag(String mesosClusterContainerId) {
        for (String image : config.dindImages) {
            try {
                Thread.sleep(2000); // we have to wait
            } catch (InterruptedException e) {
            }

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(mesosClusterContainerId)
                    .withAttachStdout(true).withCmd("docker", "pull", "private-registry:5000/" + image + ":systemtest").exec();
            InputStream execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            MatcherAssert.assertThat(DockerUtil.consumeInputStream(execCmdStream), Matchers.containsString("Download complete"));

            execCreateCmdResponse = dockerClient.execCreateCmd(mesosClusterContainerId)
                    .withAttachStdout(true).withCmd("docker", "tag", "private-registry:5000/" + image + ":systemtest", image + ":latest").exec();

            execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            DockerUtil.consumeInputStream(execCmdStream);
        }
    }

    void pushDindImagesToPrivateRegistry() {
        for (String image : config.dindImages) {
            String imageWithPrivateRepoName = "localhost:" + config.privateRegistryPort + "/" + image;
            MesosCluster.LOGGER.debug("*****************************         Tagging image \"" + imageWithPrivateRepoName + "\"         *****************************");
            dockerClient.tagImageCmd(image, imageWithPrivateRepoName, "systemtest").withForce(true).exec();
            MesosCluster.LOGGER.debug("*****************************         Pushing image \"" + imageWithPrivateRepoName + ":systemtest\" to private registry        *****************************");
            InputStream responsePushImage = dockerClient.pushImageCmd(imageWithPrivateRepoName).withTag("systemtest").exec();

            MatcherAssert.assertThat(DockerUtil.consumeInputStream(responsePushImage), Matchers.containsString("The push refers to a repository"));
        }
    }

    String generateRegistryContainerName() {
        return "registry_" + new SecureRandom().nextInt();
    }

    File createRegistryStorageDirectory() {
        File registryStorageRootDir = new File(".registry");

        if (!registryStorageRootDir.exists()) {
            MesosCluster.LOGGER.info("The private registry storage root directory doesn't exist, creating one...");
            registryStorageRootDir.mkdir();
        }
        return registryStorageRootDir;
    }

    String startPrivateRegistryContainer() {
        final String REGISTRY_IMAGE_NAME = "registry";
        final String REGISTRY_TAG = "0.9.1";

        dockerUtil.pullImage(REGISTRY_IMAGE_NAME, REGISTRY_TAG);

        CreateContainerCmd command = dockerClient.createContainerCmd(REGISTRY_IMAGE_NAME + ":" + REGISTRY_TAG)
                .withName(generateRegistryContainerName())
                .withExposedPorts(ExposedPort.parse("5000"))
                .withEnv("STORAGE_PATH=/var/lib/registry")
                .withVolumes(new Volume("/var/lib/registry"))
                .withBinds(Bind.parse(createRegistryStorageDirectory().getAbsolutePath() + ":/var/lib/registry:rw"))
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.privateRegistryPort + ":5000"));

        return dockerUtil.createAndStart(command);
    }
}