package org.apache.mesos.mini;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

import java.net.URI;

public class MesosClusterConfig {

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String[] slaveResources;
    public final URI dockerHost;
    public final Integer mesosMasterPort;
    public final Integer privateRegistryPort;
    public final String[] dindImages;

    private MesosClusterConfig(DockerClient dockerClient, int numberOfSlaves, String[] slaveResources, URI dockerHost, Integer mesosMasterPort, String[] dindImages, Integer privateRegistryPort) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
        this.slaveResources = slaveResources;
        this.dockerHost = dockerHost;
        this.mesosMasterPort = mesosMasterPort;
        this.dindImages = dindImages;
        this.privateRegistryPort = privateRegistryPort;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        int numberOfSlaves = 3;
        String[] slaveResources = new String[]{};
        URI dockerHost;
        Integer mesosMasterPort = Integer.valueOf(5050);
        Integer privateRegistryPort = Integer.valueOf(5000);
        String[] dindImages = new String[]{};

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

        public Builder slaveResources(String[] slaveResources) {
            this.slaveResources = slaveResources;
            return this;
        }

        public Builder privateRegistryPort(int port){
            this.privateRegistryPort = port;
            return this;
        }

        public Builder masterPort(int port) {
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

            if (slaveResources.length != numberOfSlaves) {
                throw new IllegalStateException("Please provide one resource config for each slave");
            }


            if (dockerClient == null) {
                defaultDockerClient();
                if (dockerClient == null) {
                    throw new IllegalStateException("Specify a docker client");
                }
            }

            return new MesosClusterConfig(dockerClient, numberOfSlaves, slaveResources, dockerHost, mesosMasterPort, dindImages, privateRegistryPort);
        }


        public Builder dockerInDockerImages(String[] dindImages) {
            this.dindImages = dindImages;
            return this;
        }
    }


}
