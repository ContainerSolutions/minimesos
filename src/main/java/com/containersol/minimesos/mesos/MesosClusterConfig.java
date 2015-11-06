package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;

import java.util.Map;
import java.util.TreeMap;

@Deprecated
public class MesosClusterConfig {

    public static final String MESOS_MASTER_IMAGE = "containersol/mesos-master";
    public static final String MESOS_SLAVE_IMAGE = "containersol/mesos-agent";
    public static final String MESOS_IMAGE_TAG = "0.25.0-0.2.70.ubuntu1404";

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String[] slaveResources;
    public final Integer mesosMasterPort;
    public final String zkUrl;
    public final String mesosMasterImage;
    public final String mesosSlaveImage;
    public final String mesosImageTag;
    public final Boolean exposedHostPorts;

    public final Map<String, String> extraEnvironmentVariables;

    @Deprecated
    private MesosClusterConfig(
            DockerClient dockerClient,
            int numberOfSlaves,
            String[] slaveResources,
            Integer mesosMasterPort,
            Map<String, String> extraEnvironmentVariables,
            String zkUrl,
            String mesosMasterImage,
            String mesosSlaveImage,
            String mesosImageTag,
            Boolean exposedHostPorts
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
        this.exposedHostPorts = exposedHostPorts;
    }

    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        String[] slaveResources = new String[]{};
        Integer mesosMasterPort = 5050;
        String zkUrl = null;
        Map<String, String> extraEnvironmentVariables = new TreeMap<>();
        String mesosMasterImage = MESOS_MASTER_IMAGE;
        String mesosSlaveImage = MESOS_SLAVE_IMAGE;
        String mesosImageTag = MESOS_IMAGE_TAG;
        Boolean exposedHostPorts = false;

        private Builder() {

        }

        public Builder dockerClient(DockerClient dockerClient) {
            this.dockerClient = dockerClient;
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

        public Builder extraEnvironmentVariables(Map<String, String> extraEnvironmentVariables) {
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

        public Builder exposedHostPorts(Boolean exposedHostPorts) {
            this.exposedHostPorts = exposedHostPorts;
            return this;
        }

        public Builder defaultDockerClient() {
            this.dockerClient = DockerClientFactory.build();
            return this;
        }

        public MesosClusterConfig build() {

            if (dockerClient == null) {
                defaultDockerClient();
                if (dockerClient == null) {
                    throw new IllegalStateException("Specify a docker dockerClient");
                }
            }

            if (zkUrl == null) {
                zkUrl = "mesos";
            }

            return new MesosClusterConfig(dockerClient, slaveResources.length, slaveResources, mesosMasterPort, extraEnvironmentVariables, zkUrl, mesosMasterImage, mesosSlaveImage, mesosImageTag, exposedHostPorts);
        }
    }

    public int getNumberOfSlaves() {
        return numberOfSlaves;
    }

}
