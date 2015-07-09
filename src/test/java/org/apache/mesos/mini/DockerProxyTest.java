package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.mesos.mini.docker.DockerProxy;
import org.apache.mesos.mini.docker.DockerUtil;
import org.junit.Assert;
import org.junit.Test;

public class DockerProxyTest {

    @Test
    public void testInstantiate() throws InterruptedException, UnirestException {
        DockerClient dockerClient = getDockerClient();
        DockerProxy proxy = new DockerProxy(dockerClient);
        proxy.startProxy();
        DockerUtil dockerUtil = new DockerUtil(dockerClient);
        dockerUtil.pullImage("tutum/hello-world", "latest");
        CreateContainerCmd command = dockerClient.createContainerCmd("tutum/hello-world").withPortBindings(PortBinding.parse("0.0.0.0:80:80"));
        String containerId = dockerUtil.createAndStart(command);
        String ipAddress = dockerClient.inspectContainerCmd(containerId).exec().getNetworkSettings().getIpAddress();
        String url = "http://" + ipAddress + ":80";
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    private DockerClient getDockerClient() {
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
        return DockerClientBuilder.getInstance(config).build();
    }
}
