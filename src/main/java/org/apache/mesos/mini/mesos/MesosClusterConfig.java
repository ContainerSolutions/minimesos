package org.apache.mesos.mini.mesos;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;

public class MesosClusterConfig {

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String[] slaveResources;
    public final Integer mesosMasterPort;
    public final Integer privateRegistryPort;
    public final Integer proxyPort;

    private MesosClusterConfig(DockerClient dockerClient, int numberOfSlaves, String[] slaveResources, Integer mesosMasterPort, Integer privateRegistryPort, Integer proxyPort) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
        this.slaveResources = slaveResources;
        this.mesosMasterPort = mesosMasterPort;
        this.privateRegistryPort = privateRegistryPort;
        this.proxyPort = proxyPort;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        DockerClient dockerClient;
        int numberOfSlaves = 3;
        String[] slaveResources = new String[]{};
        Integer mesosMasterPort = Integer.valueOf(5050);
        Integer privateRegistryPort = Integer.valueOf(5000);
        Integer proxyPort = Integer.valueOf(8888);


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

        public Builder proxyPort(int port){
            this.proxyPort = port;
            return this;
        }

        public Builder masterPort(int port) {
            this.mesosMasterPort = Integer.valueOf(port);
            return this;
        }


        public Builder defaultDockerClient() {
            DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();

            String dockerHostEnv = System.getenv("DOCKER_HOST");
            if (StringUtils.isBlank(dockerHostEnv)) {
                builder.withUri("unix:///var/run/docker.sock");
            }

            DockerClientConfig config = builder.build();

            if (config.getUri().getScheme().startsWith("http") && !System.getProperty("os.name").equals("Linux")) {
                HttpHost proxy = new HttpHost(config.getUri().getHost(), this.proxyPort);
                Unirest.setProxy(proxy);
                Unirest.setTimeouts(5000L, 5000L);
            }
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

            return new MesosClusterConfig(dockerClient, numberOfSlaves, slaveResources, mesosMasterPort, privateRegistryPort, proxyPort);
        }

    }

}
