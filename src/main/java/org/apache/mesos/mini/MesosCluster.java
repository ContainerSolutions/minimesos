package org.apache.mesos.mini;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.InternalServerErrorException;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosContainer;
import org.apache.mesos.mini.state.State;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.apache.mesos.mini.util.Predicate;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

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

    private final List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<>());

    private final MesosClusterConfig config;

    private MesosContainer mesosContainer;

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
    }

    /**
     * Starts the Mesos cluster and its containers
     */
    public void start() {
        LOGGER.info("Starting Mesos cluster");

        try {
            LOGGER.info("Starting Mesos Local");
            mesosContainer = new MesosContainer(config.dockerClient, this.config);
            addAndStartContainer(mesosContainer);
            LOGGER.info("Started Mesos Local at http://" + mesosContainer.getMesosMasterURL());

            // wait until the given number of slaves are registered
            if (!System.getenv("MINI_MESOS_PROXY_HOST").isEmpty()) {
                HttpHost proxy = new HttpHost(System.getenv("MINI_MESOS_PROXY_HOST"), Integer.parseInt(System.getenv("MINI_MESOS_PROXY_PORT")), System.getenv("MINI_MESOS_PROXY_SCHEME"));
                LOGGER.info("Using proxy: " + System.getenv("MINI_MESOS_PROXY_HOST") + ":" + System.getenv("MINI_MESOS_PROXY_PORT"));
                new MesosClusterStateResponse(mesosContainer.getMesosMasterURL(), config.numberOfSlaves, proxy).waitFor();
            }
            new MesosClusterStateResponse(mesosContainer.getMesosMasterURL(), config.numberOfSlaves).waitFor();

            }
        catch (Throwable e) {
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

    public List<AbstractContainer> getContainers() {
        return Collections.unmodifiableList(containers);
    }

    public DockerClient getInnerDockerClient() {
        return this.mesosContainer.getOuterDockerClient();
    }

    public MesosClusterConfig getConfig() {
        return config;
    }
}
