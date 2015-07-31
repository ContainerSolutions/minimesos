package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.docker.DockerProxy;
import org.apache.mesos.mini.docker.ImagePusher;
import org.apache.mesos.mini.docker.PrivateDockerRegistry;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosContainer;
import org.apache.mesos.mini.state.State;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.apache.mesos.mini.util.Predicate;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Starts the mesos cluster. Responsible for setting up proxy and private docker registry. Once started, users can add
 * their own images to the private registry and start containers which will be removed when the Mesos cluster is
 * destroyed.
 */
public class MesosCluster extends ExternalResource {
    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    private final List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<AbstractContainer>());

    private final MesosClusterConfig config;

    private MesosContainer mesosContainer;

    private DockerClient innerDockerClient;

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
    }

    /**
     * Starts the Mesos cluster and its containers
     */
    public void start() {
        LOGGER.info("Starting Mesos cluster");

        try {
            LOGGER.info("Starting Proxy");
            DockerProxy dockerProxy = new DockerProxy(config.dockerClient, config.proxyPort);
            addAndStartContainer(dockerProxy);
            LOGGER.info("Started Proxy at " + dockerProxy.getIpAddress() + ":" + config.proxyPort);

            LOGGER.info("Starting Registry");
            PrivateDockerRegistry privateDockerRegistry = new PrivateDockerRegistry(config.dockerClient, this.config);
            addAndStartContainer(privateDockerRegistry);
            LOGGER.info("Started Registry at " + privateDockerRegistry.getIpAddress() + ":" + config.privateRegistryPort);

            LOGGER.info("Starting Mesos Local");
            mesosContainer = new MesosContainer(config.dockerClient, this.config, privateDockerRegistry.getContainerId());
            addAndStartContainer(mesosContainer);
            LOGGER.info("Started Mesos Local at " + mesosContainer.getMesosMasterURL());

            DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder();
            String innerDockerHost = "http://" + mesosContainer.getIpAddress() + ":2376";
            builder.withUri(innerDockerHost);
            DockerClientConfig innerDockerConfig = builder.build();
            innerDockerClient = DockerClientBuilder.getInstance(innerDockerConfig).build();

            // wait until the given number of slaves are registered
            new MesosClusterStateResponse(mesosContainer.getMesosMasterURL(), config.numberOfSlaves).waitFor();
        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);
            throw e;
        }
        LOGGER.info("Mesos cluster started");
    }

    /**
     * Stops the Mesos cluster and its containers
     */
    public void stop() {
        LOGGER.info("Stopping Mesos cluster");
        for (AbstractContainer container : this.containers) {
            LOGGER.info("Removing container [" + container.getName() + "]");
            writeLog(container.getName(), container.getContainerId());
            container.remove();
        }
        this.containers.clear();
    }

    /**
     * Start a container. This container will be removed when the Mesos cluster is shut down.
     *
     * @param container container to be started
     * @return container ID
     */
    public String addAndStartContainer(AbstractContainer container) {
        container.start();
        containers.add(container);
        return container.getContainerId();
    }

    /**
     * Inject an image from your local docker daemon into the mesos cluster.
     *
     * @param imageName The name of the image you want to push (in the format domain/image)
     * @throws DockerException when an error pulling or pushing occurs.
     */
    public void injectImage(String imageName) throws DockerException {
        ImagePusher imagePusher = new ImagePusher(config.dockerClient, "localhost" + ":" + config.privateRegistryPort, getMesosContainer().getContainerId());
        imagePusher.injectImage(imageName);
    }

    public State getStateInfo() throws UnirestException {
        String json = Unirest.get("http://" + mesosContainer.getMesosMasterURL() + "/state.json").asString().getBody();

        return State.fromJSON(json);
    }

    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get("http://" + mesosContainer.getMesosMasterURL() + "/state.json").asJson().getBody().getObject();
    }

    public MesosContainer getMesosContainer() {
        return mesosContainer;
    }

    public void waitForState(final Predicate<State> predicate, int seconds) {
        await().atMost(seconds, TimeUnit.SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    return predicate.test(MesosCluster.this.getStateInfo());
                } catch (InternalServerErrorException e) {
                    LOGGER.error(e);
                    // This probably means that the mesos cluster isn't ready yet..
                    return false;
                }
            }
        });
    }

    public void waitForState(Predicate<State> predicate) {
        waitForState(predicate, 20);
    }

    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        stop();
    }

    private void writeLog(String containerName, String containerId) {
        if (containerId == null) {
            return;
        }
        try {
            InputStream logStream = this.config.dockerClient.logContainerCmd(containerId).withStdOut().exec();
            Files.copy(logStream, Paths.get(containerName + ".log"));
        } catch (IOException e) {
            LOGGER.error("Could not write logs of container " + containerName, e);
        }
    }

    public List<AbstractContainer> getContainers() {
        return Collections.unmodifiableList(containers);
    }
}
