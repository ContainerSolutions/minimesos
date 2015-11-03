package com.containersol.minimesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.marathon.MarathonClient;
import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersol.minimesos.mesos.MesosClusterConfig;
import com.containersol.minimesos.mesos.MesosMaster;
import com.containersol.minimesos.mesos.MesosSlave;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.util.MesosClusterStateResponse;
import com.containersol.minimesos.util.Predicate;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    private static File miniMesosFile = new File(System.getProperty("minimesos.dir"), "minimesos.cluster");

    private static DockerClient dockerClient = DockerClientFactory.build();

    private final List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<>());

    private final MesosClusterConfig config;

    private MesosSlave[] mesosSlaves;

    protected MesosMaster mesosMasterContainer;

    protected String zkUrl;

    protected ZooKeeper zkContainer;

    private String clusterId;

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
        this.clusterId = Integer.toUnsignedString(new SecureRandom().nextInt());
    }

    /**
     * Starts the Mesos cluster and its containers
     */
    public void start() {
        this.zkContainer = new ZooKeeper(this.config.dockerClient, clusterId);
        addAndStartContainer(this.zkContainer);

        this.zkUrl = "zk://" + this.zkContainer.getIpAddress() + ":2181/" + this.config.zkUrl;
        this.mesosMasterContainer = new MesosMaster(this.config.dockerClient, this.zkUrl, this.config.mesosMasterImage, this.config.mesosImageTag, clusterId, this.config.extraEnvironmentVariables, this.config.exposedHostPorts);
        addAndStartContainer(this.mesosMasterContainer);

        try {
            mesosSlaves = new MesosSlave[config.getNumberOfSlaves()];
            for (int i = 0; i < this.config.getNumberOfSlaves(); i++) {
                mesosSlaves[i] = new MesosSlave(this.config.dockerClient, config.slaveResources[i], "5051", this.zkUrl, mesosMasterContainer.getContainerId(), this.config.mesosSlaveImage, this.config.mesosImageTag, clusterId);
                addAndStartContainer(mesosSlaves[i]);
            }
            // wait until the given number of slaves are registered
            new MesosClusterStateResponse(this.mesosMasterContainer.getIpAddress() + ":5050", config.numberOfSlaves).waitFor();
        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);
        }

        Marathon marathon = new Marathon(this.config.dockerClient, clusterId, this.zkContainer, this.config.exposedHostPorts);
        addAndStartContainer(marathon);

        LOGGER.info("http://" + this.mesosMasterContainer.getIpAddress() + ":5050");
    }

    /**
     * Stops the Mesos cluster and its containers
     */
    public void stop() {
        for (AbstractContainer container : this.containers) {
            LOGGER.debug("Removing container [" + container.getContainerId() + "]");
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
        String json = Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":5050" + "/state.json").asString().getBody();

        return State.fromJSON(json);
    }

    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":5050" + "/state.json").asJson().getBody().getObject();
    }

    public Map<String, String> getFlags() throws UnirestException {
        JSONObject flagsJson = this.getStateInfoJSON().getJSONObject("flags");
        Map<String, String> flags = new TreeMap<String, String>();
        for (Object key : flagsJson.keySet()) {
            String keyString = (String) key;
            String value = flagsJson.getString(keyString);
            flags.put(keyString, value);
        }
        return flags;
    }

    @Override
    protected void before() throws Throwable {
        start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                destroyContainers(clusterId);
            }
        });
    }

    private static void destroyContainers(String clusterId) {
        DockerClient dockerClient = DockerClientFactory.build();
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            if (container.getNames()[0].contains(clusterId)) {
                dockerClient.removeContainerCmd(container.getId()).withForce().withRemoveVolumes(true).exec();
            }
        }
        LOGGER.info("Destroyed minimesos cluster " + clusterId);
    }

    public List<AbstractContainer> getContainers() {
        return containers;
    }

    public MesosSlave[] getSlaves() {
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

    public String getClusterId() {
        return clusterId;
    }

    public static String getContainerIp(String clusterId, String role) {
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            if (container.getNames()[0].contains("minimesos-" + role) && container.getNames()[0].contains(clusterId + "-")) {
                return dockerClient.inspectContainerCmd(container.getId()).exec().getNetworkSettings().getIpAddress();
            }
        }
        return null;
    }

    public static void destroy() {
        String clusterId = readClusterId();

        String marathonIp = getContainerIp(clusterId, "marathon");
        if (marathonIp != null) {
            MarathonClient.killAllApps(marathonIp);
        }

        if (clusterId != null) {
            destroyContainers(clusterId);
            miniMesosFile.deleteOnExit();
        }
    }

    public static String readClusterId() {
        try {
            return IOUtils.toString(new FileReader(miniMesosFile));
        } catch (IOException e) {
            return null;
        }
    }

    public static void printMasterIp(String clusterId) {
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            for (String name : container.getNames()) {
                if (name.contains("minimesos-master-" + clusterId)) {
                    String ipAddress = dockerClient.inspectContainerCmd(container.getId()).exec().getNetworkSettings().getIpAddress();
                    LOGGER.info("http://" + ipAddress + ":5050");
                    return;
                }
            }
        }
        LOGGER.warn("No running cluster found.");
    }

}
