package com.containersol.minimesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.marathon.MarathonClient;
import com.containersol.minimesos.mesos.*;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.util.MesosClusterStateResponse;
import com.containersol.minimesos.util.Predicate;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Starts the mesos cluster. Responsible for setting up a private docker registry. Once started, users can add
 * their own images to the private registry and start containers which will be removed when the Mesos cluster is
 * destroyed.
 */
public class MesosCluster extends ExternalResource {

    public static final String MINIMESOS_DIR_PROPERTY = "minimesos.dir";
    public static final String MINIMESOS_FILE_PROPERTY = "minimesos.cluster";

    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    private static DockerClient dockerClient = DockerClientFactory.build();

    private final List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<>());

    private MesosClusterConfig config;
    private ClusterArchitecture clusterArchitecture;

    private static String clusterId;

    /**
     * Old configuration instantiation.
     * @deprecated use {@link #MesosCluster(ClusterArchitecture)} instead.
     */
    @Deprecated
    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
        clusterId = Integer.toUnsignedString(new SecureRandom().nextInt());
    }

    /**
     * Create a new cluster with a specified cluster architecture.
     * @param clusterArchitecture Represents the layout of the cluster. See {@link ClusterArchitecture} and {@link ClusterUtil}
     */
    public MesosCluster(ClusterArchitecture clusterArchitecture) {
        this.clusterArchitecture = clusterArchitecture;
        clusterId = Integer.toUnsignedString(new SecureRandom().nextInt());
    }

    /**
     * Starts the Mesos cluster and its containers
     */
    public void start() {
        if (config == null && clusterArchitecture == null) {
            throw new ClusterArchitecture.MesosArchitectureException("No cluster architecture specified");
        }
        // If the user is still using the old configuration method, then retain their old options. This should prevent the CLI API from breaking.
        if (config != null) {
            ClusterArchitecture.Builder builder = new ClusterArchitecture.Builder();
            builder.withZooKeeper().withMaster( zkContainer ->
                    new MesosMasterExtended(this.config.dockerClient, zkContainer, this.config.mesosMasterImage, this.config.mesosImageTag, clusterId, this.config.extraEnvironmentVariables, this.config.exposedHostPorts)
            );
            try {
                for (int i = 0; i < this.config.getNumberOfSlaves(); i++) {
                    final String slaveResource = config.slaveResources[i];
                    builder.withSlave( zkContainer ->
                            new MesosSlaveExtended(this.config.dockerClient, slaveResource, "5051", zkContainer, this.config.mesosSlaveImage, this.config.mesosImageTag, clusterId)
                    );
                }

                builder.withContainer(zooKeeper -> new Marathon(dockerClient, clusterId, (ZooKeeper) zooKeeper, this.config.exposedHostPorts), ClusterContainers.Filter.zooKeeper());

                this.clusterArchitecture = builder.build();
            } catch (Throwable e) {
                LOGGER.error("Error during startup", e);
            }
        }

        clusterArchitecture.getClusterContainers().getContainers().forEach(this::addAndStartContainer);
        // wait until the given number of slaves are registered
        new MesosClusterStateResponse(getMesosMasterContainer().getIpAddress() + ":" + MesosMaster.MESOS_MASTER_PORT, getSlaves().length).waitFor();
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
        String json = Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":5050" +"/state.json").asString().getBody();

        return State.fromJSON(json);
    }

    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":5050" + "/state.json").asJson().getBody().getObject();
    }

    public Map<String, String> getFlags() throws UnirestException {
        JSONObject flagsJson = this.getStateInfoJSON().getJSONObject("flags");
        Map<String, String> flags = new TreeMap<>();
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
        return clusterArchitecture.getClusterContainers().getContainers();
    }

    public MesosSlave[] getSlaves() {
           return clusterArchitecture.getClusterContainers().getContainers().stream().filter(ClusterContainers.Filter.mesosSlave()).collect(Collectors.toList()).toArray(new MesosSlave[0]);
    }

    @Override
    protected void after() {
        stop();
    }

    @Deprecated
    public MesosClusterConfig getConfig() {
        return config;
    }

    public MesosMaster getMesosMasterContainer() {
        return (MesosMaster) clusterArchitecture.getClusterContainers().getOne(ClusterContainers.Filter.mesosMaster()).get();
    }

    public String getZkUrl() {
        return MesosContainer.getFormattedZKAddress(getZkContainer());
    }

    public ZooKeeper getZkContainer() {
        return (ZooKeeper) clusterArchitecture.getClusterContainers().getOne(ClusterContainers.Filter.zooKeeper()).get();
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

    public static String getClusterId() {
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

    /**
     * Check existence of a running minimesos master container
     * @param clusterId String
     * @return boolean
     */
    public static boolean isUp(String clusterId) {
        if (clusterId != null) {
            DockerClient dockerClient = DockerClientFactory.build();
            List<Container> containers = dockerClient.listContainersCmd().exec();
            for (Container container : containers) {
                for (String name : container.getNames()) {
                    if (name.contains("minimesos-master-" + clusterId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void destroy() {
        String clusterId = readClusterId();

        String marathonIp = getContainerIp(clusterId, "marathon");
        if (marathonIp != null) {
            MarathonClient.killAllApps(marathonIp);
        }

        if (clusterId != null) {
            destroyContainers(clusterId);
            File minimesosFile = getMinimesosFile();
            if( minimesosFile.exists() ) {
                minimesosFile.deleteOnExit();
            }
        } else {
            LOGGER.info("Minimesos cluster is not running");
        }
    }

    public static String readClusterId() {
        try {
            return IOUtils.toString(new FileReader(getMinimesosFile()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @return never null
     */
    private static File getMinimesosFile() {
        return new File( getMinimesosDir(), MINIMESOS_FILE_PROPERTY);
    }

    private static File getMinimesosDir() {
        String sp = System.getProperty(MINIMESOS_DIR_PROPERTY);
        if( sp == null ) {
            sp = System.getProperty("user.dir") + "/.minimesos";
        }
        return new File( sp );
    }

    public void writeClusterId() {
        File minimesosDir = getMinimesosDir();
        try {
            FileUtils.forceMkdir(minimesosDir);
            Files.write(Paths.get(minimesosDir.getAbsolutePath() + "/" + MINIMESOS_FILE_PROPERTY), MesosCluster.getClusterId().getBytes());
        } catch (IOException ie) {
            LOGGER.error("Could not write .minimesos folder", ie);
            throw new RuntimeException(ie);
        }
    }

    public static void checkStateFile(String clusterId) {
        if (clusterId != null && !isUp(clusterId)) {
            File minimesosFile = getMinimesosFile();
            if (minimesosFile.delete()) {
                LOGGER.info("Invalid state file removed");
            } else {
                LOGGER.info("Cannot remove invalid state file " + minimesosFile.getAbsolutePath());
            }
        }
    }

    public static void printServiceUrl(String clusterId, String serviceName, boolean exposedHostPorts) {
        String dockerHostIp = System.getenv("DOCKER_HOST_IP");
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            for (String name : container.getNames()) {
                if (name.contains("minimesos-" + serviceName + "-" + clusterId)) {
                    String uri, ip;
                    if (!exposedHostPorts || dockerHostIp.isEmpty()) {
                        InspectContainerResponse.NetworkSettings containerNetworkSettings;
                        containerNetworkSettings = dockerClient.inspectContainerCmd(container.getId()).exec().getNetworkSettings();
                        ip = containerNetworkSettings.getIpAddress();
                    } else {
                        ip = dockerHostIp;
                    }
                    switch (serviceName) {
                        case "master":
                            uri = "Master http://" + ip + ":" + MesosMaster.MESOS_MASTER_PORT;
                            break;
                        case "marathon":
                            uri = "Marathon http://" + ip + ":" + Marathon.MARATHON_PORT;
                            break;
                        default:
                            uri = "Unknown service type '" + serviceName + "'";
                    }
                    LOGGER.info(uri);
                    return;
                }
            }
        }
    }

}
