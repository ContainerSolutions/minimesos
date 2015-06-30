package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;

import java.security.SecureRandom;

public class MesosCluster {

    public String mesosBaseImage = "mesosphere/mesos:0.22.1-1.0.ubuntu1404";

    final private MesosClusterConfig config;
    private String containerName;

    public CreateContainerResponse createContainerResponse;
    public StartContainerCmd startContainerCmd;

    public MesosCluster(int numberOfSlaves) {
        this(MesosClusterConfig.builder().defaultDockerClient().numberOfSlaves(numberOfSlaves).build());
    }

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
    }


    public void start() {

        containerName = "generated_" + new SecureRandom().nextInt();

        createContainerResponse = config.dockerClient.createContainerCmd(mesosBaseImage).withCmd("mesos-local", "--num_slaves=" + config.numberOfSlaves)
                .withName(containerName).exec();

        startContainerCmd = config.dockerClient.startContainerCmd(createContainerResponse.getId());
        startContainerCmd.exec();

    }

    public void stop() {
        try {
            config.dockerClient.removeContainerCmd(containerName).withForce().exec();
        } catch (DockerException ignore) {
            ignore.printStackTrace();
        }

    }


}
