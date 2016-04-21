package com.containersol.minimesos.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import java.io.File;
import java.util.Map;
import java.util.Properties;

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
    private static String dockerUri;
    private static String dockerCertPath;
    private static Properties properties = System.getProperties();
    private static Map<String,String> environment = System.getenv();

    public static DockerClient build() {
        DockerClientConfig config = populateConfigBuilder(configBuilder).build();
        dockerClient = DockerClientBuilder.getInstance(config).build();
        return dockerClient;
    }

    public static DockerClient build(Properties argProperties) {
        properties = argProperties;
        return build();
    }

    public static DockerClient build(Map<String,String> env) {
        environment = env;
        return build();
    }

    public static DockerClientConfigBuilder populateConfigBuilder(DockerClientConfigBuilder builder) {
        builder.withVersion("");
        builder.withDockerCertPath("");

        dockerHostEnv = environment.get("DOCKER_HOST");
        dockerCertPathEnv = environment.get("DOCKER_CERT_PATH");
        dockerHostPrp = properties.getProperty("docker.io.url");
        dockerCertPathPrp = properties.getProperty("docker.io.dockerCertPath");
        dockerUri = "unix:///var/run/docker.sock";
        
        if (dockerHostEnv != null) {
            dockerUri = dockerHostEnv;
        } else if (dockerHostPrp != null) {
            dockerUri = dockerHostPrp;
            builder.withProperties(properties);
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
