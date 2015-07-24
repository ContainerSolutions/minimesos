package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.docker.DockerProxy;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

public class DockerProxyTest {

    public static final int PROXY_PORT = 8777;

    private static HelloWorldContainer helloWorld;
    private static DockerProxy proxy;

    @AfterClass
    public static void callShutdownHook() {
        helloWorld.remove();
        proxy.remove();
    }

    private static DockerClient getDockerClient() {
        DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();

        String dockerHostEnv = System.getenv("DOCKER_HOST");
        if (StringUtils.isBlank(dockerHostEnv)) {
            builder.withUri("unix:///var/run/docker.sock");
        }

        DockerClientConfig config = builder.build();

        if (config.getUri().getScheme().startsWith("http")) {
            HttpHost proxy = new HttpHost(config.getUri().getHost(), PROXY_PORT);
            Unirest.setProxy(proxy);
        }
        return DockerClientBuilder.getInstance(config).build();
    }

    @Test
    public void testInstantiate() throws InterruptedException, UnirestException {
        DockerClient dockerClient = getDockerClient();
        proxy = new DockerProxy(dockerClient, PROXY_PORT);
        proxy.start();

        helloWorld = new HelloWorldContainer(dockerClient);
        helloWorld.start();

        String ipAddress = helloWorld.getIpAddress();
        String url = "http://" + ipAddress + ":" + HelloWorldContainer.PORT;
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    class HelloWorldContainer extends AbstractContainer {

        public static final String HELLO_WORLD_IMAGE = "tutum/hello-world";
        public static final int PORT = 80;

        protected HelloWorldContainer(DockerClient dockerClient) {
            super(dockerClient);
        }

        @Override
        protected void pullImage() {
            pullImage(HELLO_WORLD_IMAGE, "latest");
        }

        @Override
        protected CreateContainerCmd dockerCommand() {
            return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withName("hello-world_" + new SecureRandom().nextInt());
        }
    }
}
