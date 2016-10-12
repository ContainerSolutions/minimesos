package com.containersol.minimesos.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.commons.lang.StringUtils;

/**
 * Factory for creating {@link DockerClient}s
 */
public class DockerClientFactory {

    private static DockerClient dockerClient;

    public static DockerClient build() {
        if (dockerClient == null) {
            DefaultDockerClientConfig.Builder builder = new DefaultDockerClientConfig.Builder();
            builder = builder.withApiVersion("1.12");

            String dockerHostEnv = System.getenv("DOCKER_HOST");
            if (StringUtils.isBlank(dockerHostEnv)) {
                builder.withDockerHost("unix:///var/run/docker.sock");
            }

            DockerClientConfig config = builder.build();
            dockerClient = DockerClientBuilder.getInstance(config).build();
        }
        return dockerClient;
    }

}
