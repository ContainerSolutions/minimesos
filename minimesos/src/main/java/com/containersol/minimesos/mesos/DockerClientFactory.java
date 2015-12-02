package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.commons.lang.StringUtils;

/**
 * Factory for creating {@link DockerClient}s
 */
public class DockerClientFactory {

    public static DockerClient build() {

        DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();
        builder.withVersion("");

        String dockerHostEnv = System.getenv("DOCKER_HOST");
        if (StringUtils.isBlank(dockerHostEnv)) {
            builder.withUri("unix:///var/run/docker.sock");
        }

        DockerClientConfig config = builder.build();
        return DockerClientBuilder.getInstance(config).build();
    }
}
