package org.apache.mesos.mini;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class MesosClusterConfig {

    public final DockerClient dockerClient;
    public final int numberOfSlaves;

    public MesosClusterConfig(DockerClient dockerClient, int numberOfSlaves) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        int numberOfSlaves = 3;

        private Builder() {

        }

        public Builder dockerClient(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
            return this;
        }

        public Builder numberOfSlaves(int numberOfSlaves) {
            this.numberOfSlaves = numberOfSlaves;
            return this;
        }


        public Builder defaultDockerClient() {

            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
//                    .withVersion("1.18")
//                    .withUri(hostAddress)
                    .build();

            this.dockerClient = DockerClientBuilder.getInstance(config).build();
            return this;
        }


        public MesosClusterConfig build() {

            if (numberOfSlaves <= 0) {
                throw new IllegalStateException("At least one slave is required to run a mesos cluster");
            }

            if (dockerClient == null) {
                throw new IllegalStateException("Specify a docker ");
            }

            return new MesosClusterConfig(dockerClient, numberOfSlaves);
        }


    }


}
