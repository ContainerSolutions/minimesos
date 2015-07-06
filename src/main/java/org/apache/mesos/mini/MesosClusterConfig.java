package org.apache.mesos.mini;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.net.URI;

public class MesosClusterConfig {

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String slaveResources;
    public final URI dockerHost;
    public final Integer mesosMasterPort;

    private MesosClusterConfig(DockerClient dockerClient, int numberOfSlaves, String slaveResources, URI dockerHost, Integer mesosMasterPort) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
        this.slaveResources = slaveResources;
        this.dockerHost = dockerHost;
        this.mesosMasterPort = mesosMasterPort;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        int numberOfSlaves = 3;
        String slaveResources;
        private URI dockerHost;
        Integer mesosMasterPort = Integer.valueOf(5050);


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

        public Builder slaveResources(String slaveResources) {
            this.slaveResources = slaveResources;
            return this;
        }


        public Builder masterPort(int port){
            this.mesosMasterPort = Integer.valueOf(port);
            return this;
        }

        public Builder defaultDockerClient() {

            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
//                    .withVersion("1.18")
//                    .withUri(hostAddress)
                    .build();

            this.dockerClient = DockerClientBuilder.getInstance(config).build();
            this.dockerHost = config.getUri();
            return this;
        }


        public MesosClusterConfig build() {

            if (numberOfSlaves <= 0) {
                throw new IllegalStateException("At least one slave is required to run a mesos cluster");
            }

            if (dockerClient == null) {
                throw new IllegalStateException("Specify a docker ");
            }

            return new MesosClusterConfig(dockerClient, numberOfSlaves, slaveResources, dockerHost, mesosMasterPort);
        }


    }


}
