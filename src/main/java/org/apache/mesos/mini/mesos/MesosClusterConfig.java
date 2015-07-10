package org.apache.mesos.mini.mesos;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;

import java.io.File;
import java.net.URI;

public class MesosClusterConfig {

    public final DockerClient dockerClient;
    public final int numberOfSlaves;
    public final String[] slaveResources;
    public final URI dockerHost;
    public final Integer mesosMasterPort;
    public final Integer privateRegistryPort;
    public final String[] dindImages;
    public final ImageToBuild[] imagesToBuild;

    private MesosClusterConfig(DockerClient dockerClient, int numberOfSlaves, String[] slaveResources, URI dockerHost, Integer mesosMasterPort, String[] dindImages, Integer privateRegistryPort, ImageToBuild[] imagesToBuild) {
        this.dockerClient = dockerClient;
        this.numberOfSlaves = numberOfSlaves;
        this.slaveResources = slaveResources;
        this.dockerHost = dockerHost;
        this.mesosMasterPort = mesosMasterPort;
        this.dindImages = dindImages;
        this.privateRegistryPort = privateRegistryPort;
        this.imagesToBuild = imagesToBuild;
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
        ImageToBuild[] imagesToBuild = new ImageToBuild[]{};



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
        public Builder imagesToBuild(ImageToBuild ... imagesToBuild){
            this.imagesToBuild = imagesToBuild;
            return this;
        }


        public Builder defaultDockerClient() {
            DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();

            String dockerHostEnv = System.getenv("DOCKER_HOST");
            if (StringUtils.isBlank(dockerHostEnv)) {
                builder.withUri("unix:///var/run/docker.sock");
            }

            DockerClientConfig config = builder.build();

            if (config.getUri().getScheme().startsWith("http")) {
                HttpHost proxy = new HttpHost(config.getUri().getHost(), 8888);
                Unirest.setProxy(proxy);
            }
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
                    throw new IllegalStateException("Specify a docker dockerClient");
                }
            }

            return new MesosClusterConfig(dockerClient, numberOfSlaves, slaveResources, dockerHost, mesosMasterPort, dindImages, privateRegistryPort, imagesToBuild);
        }


        public Builder dockerInDockerImages(String[] dindImages) {
            this.dindImages = dindImages;
            return this;
        }
    }

    public static class ImageToBuild {
        final File srcFolder;
        final String tag;

        public ImageToBuild(File srcFolder, String tag) {
            this.srcFolder = srcFolder;
            this.tag = tag;
        }
    }

}
