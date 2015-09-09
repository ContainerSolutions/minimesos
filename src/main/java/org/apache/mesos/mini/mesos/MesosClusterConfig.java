package org.apache.mesos.mini.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.TreeMap;

public class MesosClusterConfig {

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String[] slaveResources;
    public final Integer mesosMasterPort;
    public final Integer privateRegistryPort;
    public final Map<String,String> extraEnvironmentVariables;

    private MesosClusterConfig(
            DockerClient dockerClient,
            int numberOfSlaves,
            String[] slaveResources,
            Integer mesosMasterPort,
            Integer privateRegistryPort,
            Map<String,String> extraEnvironmentVariables
    ) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
        this.slaveResources = slaveResources;
        this.mesosMasterPort = mesosMasterPort;
        this.privateRegistryPort = privateRegistryPort;
        this.extraEnvironmentVariables = extraEnvironmentVariables;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        int numberOfSlaves = 3;
        String[] slaveResources = new String[]{};
        Integer mesosMasterPort = 5050;
        Integer privateRegistryPort = 5000;
        Map<String,String> extraEnvironmentVariables = new TreeMap<>();

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
            this.mesosMasterPort = port;
            return this;
        }

        public Builder extraEnvironmentVariables(Map<String,String> extraEnvironmentVariables) {
            this.extraEnvironmentVariables = extraEnvironmentVariables;
            return this;
        }

        public Builder defaultDockerClient() {
            DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();

            String dockerHostEnv = System.getenv("DOCKER_HOST");
            if (StringUtils.isBlank(dockerHostEnv)) {
                builder.withUri("unix:///var/run/docker.sock");
            }

            DockerClientConfig config = builder.build();

            this.dockerClient = DockerClientBuilder.getInstance(config).build();
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

            return new MesosClusterConfig(
                    dockerClient,
                    numberOfSlaves,
                    slaveResources,
                    mesosMasterPort,
                    privateRegistryPort,
                    extraEnvironmentVariables
            );
        }

    }

}
