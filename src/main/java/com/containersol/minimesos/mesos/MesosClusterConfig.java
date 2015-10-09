package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;

import java.util.Map;
import java.util.TreeMap;

public class MesosClusterConfig {

    public static final String MESOS_MASTER_IMAGE = "mesosphere/mesos-master";
    public static final String MESOS_SLAVE_IMAGE = "mesosphere/mesos-slave";
    public static final String MESOS_IMAGE_TAG = "0.22.1-1.0.ubuntu1404";

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String[] slaveResources;
    public final Integer mesosMasterPort;
    public final String zkUrl;
    public final String mesosMasterImage;
    public final String mesosSlaveImage;
    public final String mesosImageTag;

    public final Map<String,String> extraEnvironmentVariables;

    private MesosClusterConfig(
            DockerClient dockerClient,
            int numberOfSlaves,
            String[] slaveResources,
            Integer mesosMasterPort,
            Map<String,String> extraEnvironmentVariables,
            String zkUrl,
            String mesosMasterImage,
            String mesosSlaveImage,
            String mesosImageTag
    ) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
        this.slaveResources = slaveResources;
        this.mesosMasterPort = mesosMasterPort;
        this.extraEnvironmentVariables = extraEnvironmentVariables;
        this.zkUrl = zkUrl;
        this.mesosMasterImage = mesosMasterImage;
        this.mesosSlaveImage = mesosSlaveImage;
        this.mesosImageTag = mesosImageTag;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        int numberOfSlaves = 3;
        String[] slaveResources = new String[]{};
        Integer mesosMasterPort = 5050;
        String zkUrl = null;
        Map<String,String> extraEnvironmentVariables = new TreeMap<>();
        String mesosMasterImage = MESOS_MASTER_IMAGE;
        String mesosSlaveImage = MESOS_SLAVE_IMAGE;
        String mesosImageTag = MESOS_IMAGE_TAG;

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

        public Builder masterPort(int port) {
            this.mesosMasterPort = port;
            return this;
        }

        public Builder extraEnvironmentVariables(Map<String,String> extraEnvironmentVariables) {
            this.extraEnvironmentVariables = extraEnvironmentVariables;
            return this;
        }

        public Builder zkUrl(String zkUrl) {
            this.zkUrl = zkUrl;
            return this;
        }

        public Builder mesosMasterImage(String mesosMasterImage) {
            this.mesosMasterImage = mesosMasterImage;
            return this;
        }

        public Builder mesosSlaveImage(String mesosSlaveImage) {
            this.mesosSlaveImage = mesosSlaveImage;
            return this;
        }

        public Builder mesosImageTag(String mesosImageTag) {
            this.mesosImageTag = mesosImageTag;
            return this;
        }

        public Builder defaultDockerClient() {
            this.dockerClient = DockerClientFactory.build();
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
                    throw new IllegalStateException("Specify a docker dockerClient");
                }
            }

            if (zkUrl == null) {
                zkUrl = "mesos";
            }

            return new MesosClusterConfig(dockerClient, numberOfSlaves, slaveResources, mesosMasterPort, extraEnvironmentVariables, zkUrl, mesosMasterImage, mesosSlaveImage, mesosImageTag);
        }
    }

    public int getNumberOfSlaves() {
        return numberOfSlaves;
    }
}
