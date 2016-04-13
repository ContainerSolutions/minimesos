package com.containersol.minimesos.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import java.io.File;

/**
 * Factory for creating {@link DockerClient}s
 */
public class DockerClientFactory {

    private static DockerClient dockerClient;

    public static DockerClient build() {
        if (dockerClient == null) {
            DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();
            builder.withVersion("");

            String dockerHostEnv = System.getenv("DOCKER_HOST");
            String dockerCertPathEnv = System.getenv("DOCKER_CERT_PATH");
            String dockerHostPrp = System.getProperty("docker.host");
            String dockerCertPathPrp = System.getProperty("docker.cert.path");
            String dockerUri = "unix:///var/run/docker.sock";
            builder.withDockerCertPath("");

            if (dockerHostPrp != null) {
                dockerUri = dockerHostPrp;
            }
            if (dockerHostEnv != null) {
                dockerUri = dockerHostEnv;
            }
            if (dockerCertPathPrp != null && new File(dockerCertPathPrp).exists()) {
                builder.withDockerCertPath(dockerCertPathPrp);
            }
            if (dockerCertPathEnv != null && new File(dockerCertPathEnv).exists()) {
                builder.withDockerCertPath(dockerCertPathEnv);
            }
            builder.withUri(dockerUri.replace("tcp", "https"));
            DockerClientConfig config = builder.build();
            dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        return dockerClient;
    }

}
