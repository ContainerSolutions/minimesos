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
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.docker.DockerProxy;
import org.apache.mesos.mini.docker.DockerUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class DockerProxyTest {

    @AfterClass
    public static void callShutdownHook() {
        new DockerUtil(getDockerClient()).stop();
    }

    private static DockerClient getDockerClient() {
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

    @Test
    public void testInstantiate() throws InterruptedException, UnirestException {
        DockerClient dockerClient = getDockerClient();
        DockerProxy proxy = new DockerProxy(dockerClient);
        proxy.start();

        HelloWorldContainer helloWorldContainer = new HelloWorldContainer(dockerClient);
        helloWorldContainer.start();

        String ipAddress = helloWorldContainer.getIpAddress();
        String url = "http://" + ipAddress + ":" + HelloWorldContainer.PORT;
        Assert.assertEquals(200, Unirest.get(url).asString().getStatus());
    }

    class HelloWorldContainer extends AbstractContainer {

        public static final String HELLO_WORLD_IMAGE = "tutum/hello-world";
        public static final int PORT = 9599;

        protected HelloWorldContainer(DockerClient dockerClient) {
            super(dockerClient);
        }

        @Override
        protected void pullImage() {
            dockerUtil.pullImage(HELLO_WORLD_IMAGE, "latest");
        }

        @Override
        protected CreateContainerCmd dockerCommand() {
            return dockerClient.createContainerCmd(HELLO_WORLD_IMAGE).withName("hello-world").withPortBindings(PortBinding.parse("0.0.0.0:" + PORT + ":" + PORT));
        }
    }
}
