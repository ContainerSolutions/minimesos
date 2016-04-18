package com.containersol.minimesos.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import java.io.File;

import static com.github.dockerjava.core.DockerClientConfig.*;

/**
 * Factory for creating {@link DockerClient}s
 */
public class DockerClientFactory {
    private static DockerClient dockerClient;
    private static DockerClientConfigBuilder configBuilder = new DockerClientConfigBuilder();
    private static String dockerHostEnv;
    private static String dockerCertPathEnv;
    private static String dockerHostPrp;
    private static String dockerCertPathPrp;
    private static String dockerUri = "unix:///var/run/docker.sock";
    private static String dockerCertPath;

    public static DockerClient build() {
        DockerClientConfig config = populateConfigBuilder(configBuilder).build();
        dockerClient = DockerClientBuilder.getInstance(config).build();
        return dockerClient;
    }

    public static DockerClientConfigBuilder populateConfigBuilder(DockerClientConfigBuilder builder) {
        builder.withVersion("");
        builder.withDockerCertPath("");

        dockerHostEnv = System.getenv("DOCKER_HOST");
        dockerCertPathEnv = System.getenv("DOCKER_CERT_PATH");
        dockerHostPrp = System.getProperty("docker.io.url");
        dockerCertPathPrp = System.getProperty("docker.io.dockerCertPath");

        if (dockerHostEnv != null) {
            dockerUri = dockerHostEnv;
        } else if (dockerHostPrp != null) {
            dockerUri = dockerHostPrp;
            builder.withProperties(System.getProperties());
        }
        if (dockerCertPathPrp != null && new File(dockerCertPathPrp).exists()) {
            dockerCertPath = dockerCertPathPrp;
        }
        if (dockerCertPathEnv != null && new File(dockerCertPathEnv).exists()) {
            dockerCertPath = dockerCertPathEnv;
        }
        builder.withUri(dockerUri.replace("tcp", "https"));
        if (dockerCertPath != null) {
            builder.withDockerCertPath(dockerCertPath);
        }
        return builder;
    }

    public static DockerClient getDockerClient() {
        if (dockerClient == null) {
            dockerClient = build();
        }
        return dockerClient;
    }

    public static String getDockerUri() {
        return dockerUri;
    }

    public static String getDockerCertPath() {
        return dockerCertPath;
    }
}
