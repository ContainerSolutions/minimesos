package org.apache.mesos.mini;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.docker.PrivateDockerRegistry;
import org.apache.mesos.mini.docker.ResponseCollector;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosContainer;
import org.apache.mesos.mini.state.State;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.apache.mesos.mini.util.Predicate;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Starts the mesos cluster. Responsible for setting up a private docker registry. Once started, users can add
 * their own images to the private registry and start containers which will be removed when the Mesos cluster is
 * destroyed.
 */
public class MesosCluster extends ExternalResource {
    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    private final List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<AbstractContainer>());

    private final MesosClusterConfig config;

    private MesosContainer mesosContainer;

    private static PrivateDockerRegistry privateDockerRegistry;

    private String privateRepoUrl;

    public final List<String> injectedImages = new ArrayList<>();

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
        this.privateRepoUrl = "localhost" + ":" + config.privateRegistryPort;
    }

    /**
     * Starts the Mesos cluster and its containers
     */
    public void start() {
        LOGGER.info("Starting Mesos cluster");

        try {
            if (privateDockerRegistry == null) {
                LOGGER.info("Starting Registry");
                privateDockerRegistry = new PrivateDockerRegistry(config.dockerClient, this.config);
                privateDockerRegistry.start();
                LOGGER.info("Started Registry at http://" + privateDockerRegistry.getIpAddress() + ":" + config.privateRegistryPort);
            } else {
                LOGGER.info("Registry is already running");
            }

            LOGGER.info("Starting Mesos Local");
            mesosContainer = new MesosContainer(config.dockerClient, this.config, privateDockerRegistry.getContainerId());
            addAndStartContainer(mesosContainer);
            LOGGER.info("Started Mesos Local at http://" + mesosContainer.getMesosMasterURL());

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
     * Inject an image (with tag "latest") from your local docker daemon into the mesos cluster.
     *
     * @param imageName The name of the image (without tag) you want to push (in the format domain/image)
     * @throws DockerException when an error pulling or pushing occurs.
     */
    public void injectImage(String imageName) throws DockerException {
        injectImage(imageName, "latest");
    }

    /**
     * Inject an image from your local docker daemon into the mesos cluster.
     *
     * @param imageName The name of the image you want to push (in the format domain/image)
     * @param tag The tag of image to inject (e.g. "1.0.0" or "latest")
     * @throws DockerException when an error pulling or pushing occurs.
     */
    public void injectImage(String imageName, String tag) throws DockerException {
        if (injectedImages.contains(imageName + ":" + tag)) {
            LOGGER.info("Image " + imageName + ":" + tag + " is already injected");
            return;
        }

        String imageNameWithTag = imageName + ":" + tag;
        LOGGER.info("Injecting image [" + privateRepoUrl + "/" + imageNameWithTag + "]");

        // Retag image in local docker daemon
        config.dockerClient.tagImageCmd(imageName, privateRepoUrl + "/" + imageName, tag).withForce().exec();

        // Push from local docker daemon to private registry
        InputStream responsePushImage = config.dockerClient.pushImageCmd(privateRepoUrl + "/" + imageName).withTag(tag).exec();
        String fullLog = ResponseCollector.collectResponse(responsePushImage);
        if (!successfulPush(fullLog)){
            throw new DockerException("Unable to push image: " + imageNameWithTag + "\n" + fullLog, 404);
        }

        // As mesos-local daemon, pull from private registry
        ExecCreateCmdResponse exec = config.dockerClient.execCreateCmd(getMesosContainer().getContainerId()).withAttachStdout(true).withCmd("docker", "pull", "private-registry:5000/" + imageNameWithTag).exec();
        InputStream execCmdStream = config.dockerClient.execStartCmd(exec.getId()).exec();
        fullLog = ResponseCollector.collectResponse(execCmdStream);
        if (!successfulPull(fullLog)){
            throw new DockerException("Unable to pull image: " + imageNameWithTag + "\n" + fullLog, 404);
        }

        // As mesos-local daemon, retag in local registry
        exec = config.dockerClient.execCreateCmd(getMesosContainer().getContainerId()).withAttachStdout(true).withCmd("docker", "tag", "-f", "private-registry:5000/" + imageNameWithTag, imageNameWithTag).exec();
        config.dockerClient.execStartCmd(exec.getId()).exec(); // This doesn't produce any log messages

        LOGGER.info("Succesfully injected [" + privateRepoUrl + "/" + imageNameWithTag + "]");

        injectedImages.add(imageName + ":" + tag);
    }

    public State getStateInfo() throws UnirestException, JsonParseException, JsonMappingException {
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

    private static boolean successfulPull(String fullLog) {
        return fullLog.contains("up to date") || fullLog.contains("Downloaded newer image");
    }

    private static boolean successfulPush(String fullLog) {
        return fullLog.contains("successfully pushed") || fullLog.contains("already pushed") || fullLog.contains("already exists");
    }

}
