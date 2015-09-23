package org.apache.mesos.mini;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.NotFoundException;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosMaster;
import org.apache.mesos.mini.mesos.MesosSlave;
import org.apache.mesos.mini.mesos.ZooKeeper;
import org.apache.mesos.mini.state.State;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Starts the mesos cluster. Responsible for setting up a private docker registry. Once started, users can add
 * their own images to the private registry and start containers which will be removed when the Mesos cluster is
 * destroyed.
 */
public class MesosCluster extends ExternalResource {
    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    private final List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<>());

    private final MesosClusterConfig config;

    private MesosSlave[] mesosSlaves;

    protected MesosMaster mesosMasterContainer;

    protected String zkUrl;

    protected ZooKeeper zkContainer;

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
    }

    /**
     * Starts the Mesos cluster and its containers
     */
    public void start() {
        this.zkContainer = new ZooKeeper(config.dockerClient);
        addAndStartContainer(this.zkContainer);
        LOGGER.info("Started zookeeper on " + this.zkContainer.getIpAddress());
        String zkPath = UUID.randomUUID().toString();

        this.zkUrl = "zk://" + this.zkContainer.getIpAddress() + ":2181/" + zkPath;
        this.mesosMasterContainer = new MesosMaster(config.dockerClient, this.zkContainer.getIpAddress(), zkPath);
        addAndStartContainer(this.mesosMasterContainer);
        LOGGER.info("Started mesos master on http://" + this.mesosMasterContainer.getIpAddress() + ":5050");
        try {
            LOGGER.info("Starting Mesos Local");
            mesosSlaves = new MesosSlave[config.getNumberOfSlaves()];
            for (int i = 0; i < this.config.getNumberOfSlaves(); i++) {
                mesosSlaves[i] = new MesosSlave(config.dockerClient, zkContainer.getIpAddress(), config.slaveResources[i], "5051", zkUrl);
                addAndStartContainer(mesosSlaves[i]);
                LOGGER.info("Started Mesos slave at " + mesosSlaves[i].getIpAddress());
            }

            // wait until the given number of slaves are registered
            new MesosClusterStateResponse(this.mesosMasterContainer.getIpAddress() + ":5050", config.numberOfSlaves).waitFor();
        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);

        }
        LOGGER.info("Mesos cluster started");
    }

    /**
     * Stops the Mesos cluster and its containers
     */
    public void stop() {
        for (AbstractContainer container : this.containers) {
            LOGGER.info("Removing container [" + container.getName() + "]");
            try {
                container.remove();
            } catch (NotFoundException e) {
                LOGGER.error(String.format("Cannot remove container %s, maybe it's already dead?", container.getContainerId()));
            }
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
        String json = Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":5050" +"/state.json").asString().getBody();

        return State.fromJSON(json);
    }

    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":5050" + "/state.json").asJson().getBody().getObject();
    }

    @Override
    protected void before() throws Throwable {
        start();
    }

    public List<AbstractContainer> getContainers() {
        return containers;
    }

    public MesosSlave[] getSlaves()
    {
           return mesosSlaves;
    }

    @Override
    protected void after() {
        stop();
    }

    public MesosClusterConfig getConfig() {
        return config;
    }

    public MesosMaster getMesosMasterContainer() {
        return mesosMasterContainer;
    }

    public String getZkUrl() {
        return zkUrl;
    }

    public ZooKeeper getZkContainer() {
        return zkContainer;
    }
}
