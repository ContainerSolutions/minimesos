package com.containersol.minimesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.main.MinimesosCliCommand;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.marathon.MarathonClient;
import com.containersol.minimesos.mesos.*;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.util.MesosClusterStateResponse;
import com.containersol.minimesos.util.Predicate;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.jayway.awaitility.Awaitility;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Starts the mesos cluster. Responsible for setting up a private docker registry. Once started, users can add
 * their own images to the private registry and start containers which will be removed when the Mesos cluster is
 * destroyed.
 */
public class MesosCluster extends ExternalResource {

    public static final String MINIMESOS_HOST_DIR_PROPERTY = "minimesos.host.dir";
    public static final String MINIMESOS_FILE_PROPERTY = "minimesos.cluster";

    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    private static DockerClient dockerClient = DockerClientFactory.build();

    private final String clusterId;

    private List<AbstractContainer> containers = Collections.synchronizedList(new ArrayList<>());

    /**
     * Create a new cluster with a specified cluster architecture.
     * @param clusterArchitecture Represents the layout of the cluster. See {@link ClusterArchitecture} and {@link ClusterUtil}
     */
    public MesosCluster(ClusterArchitecture clusterArchitecture) {
        if (clusterArchitecture == null) {
            throw new ClusterArchitecture.MesosArchitectureException("No cluster architecture specified");
        }

        this.containers = clusterArchitecture.getClusterContainers().getContainers();
        clusterId = Integer.toUnsignedString(new SecureRandom().nextInt());
    }

    /**
     * Starts the Mesos cluster and its containers with 60 second timeout.
     */
    public void start() {
        start(60);
    }

    /**
     * Starts the Mesos cluster and its containers with given timeout.
     *
     * @param timeoutSeconds seconds to wait until timeout
     */
    public void start(int timeoutSeconds) {
        this.containers.forEach((container) -> container.start(timeoutSeconds));
        // wait until the given number of slaves are registered
        new MesosClusterStateResponse(this).waitFor();
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
     * @param timeout in seconds
     *
     * @return container ID
     */
    public String addAndStartContainer(AbstractContainer container, int timeout) {

        container.setClusterId(clusterId);
        LOGGER.debug( String.format("Starting %s (%s) container", container.getName(), container.getContainerId()) );

        try {
            container.start(timeout);
            containers.add(container);
        } catch (Exception exc ) {
            String msg = String.format("Failed to start %s (%s) container", container.getName(), container.getContainerId());
            LOGGER.error( msg, exc );
            throw new MinimesosException( msg, exc );
        }


        return container.getContainerId();
    }

    /**
     * Retrieves JSON with Mesos Cluster master state
     *
     * @param clusterId id of the cluster
     * @return stage JSON
     */
    public static String getClusterStateInfo(String clusterId) {
        Container container = getContainer(clusterId, "master");
        return getContainerStateInfo(container);
    }

    /**
     * Retrieves JSON with Mesos state of the given container
     *
     * @param containerId ID of the container to get state from
     * @return stage JSON
     */
    public static String getContainerStateInfo(String containerId) {
        Container container = DockerContainersUtil.getContainer(dockerClient, containerId);
        return getContainerStateInfo(container);
    }

    private static String getContainerStateInfo(Container container) {

        String info = null;

        if (container != null) {

            String containerId = container.getId();
            String ip = DockerContainersUtil.getIpAddress(dockerClient, containerId);

            if (ip != null) {

                int port = container.getNames()[0].contains("minimesos-agent-") ? MesosSlave.MESOS_SLAVE_PORT : MesosMaster.MESOS_MASTER_PORT;
                String url = "http://" + ip + ":" + port + "/state.json";

                try {
                    HttpResponse<JsonNode> request = Unirest.get(url).asJson();
                    info = request.getBody().toString();
                } catch (UnirestException e) {
                    throw new MinimesosException("Failed to retrieve state from " + url, e);
                }

            } else {
                throw new MinimesosException("Cannot find container. Please verify the cluster is running using `minimesos info` command.");
            }
        }

        return info;

    }


    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get("http://" + this.getMesosMasterContainer().getIpAddress() + ":" + MesosMaster.MESOS_MASTER_PORT + "/state.json").asJson().getBody().getObject();
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
        start(MesosContainer.DEFAULT_TIMEOUT_SEC);
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
        List<AbstractContainer> slaves = containers.stream().filter(ClusterContainers.Filter.mesosSlave()).collect(Collectors.toList());
        MesosSlave[] array = new MesosSlave[slaves.size()];
        return slaves.toArray(array);
    }

    @Override
    protected void after() {
        stop();
    }

    public MesosMaster getMesosMasterContainer() {
        return (MesosMaster) getOne(ClusterContainers.Filter.mesosMaster()).get();
    }

    public ZooKeeper getZkContainer() {
        return (ZooKeeper) getOne(ClusterContainers.Filter.zooKeeper()).get();
    }

    public Marathon getMarathonContainer() {
        return (Marathon) getOne(ClusterContainers.Filter.marathon()).get();
    }

    /**
     * Optionally get one of a certain type of type T. Note, this cast will always work because we are filtering on that type.
     * If it doesn't find that type, the optional is empty so the cast doesn't need to be performed.
     *
     * @param filter A predicate that is true when an {@link AbstractContainer} in the list is of type T
     * @param <T> A container of type T that extends {@link AbstractContainer}
     * @return the first container it comes across.
     */
    private <T extends AbstractContainer> Optional<T> getOne(java.util.function.Predicate<AbstractContainer> filter) {
        return (Optional<T>) getContainers().stream().filter(filter).findFirst();
    }

    public String getClusterId() {
        return clusterId;
    }

    /**
     * Type safe retrieval of container object (based on naming convention)
     * @param clusterId ID of the cluster to search for containers
     * @param role container role in the cluster
     * @return object of clazz type, which represent the container
     */
    public static Container getContainer(String clusterId, String role) {
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            if (container.getNames()[0].contains("minimesos-" + role) && container.getNames()[0].contains(clusterId + "-")) {
                return container;
            }
        }
        return null;
    }

    public static String getContainerIp(String clusterId, String role) {
        Container container = getContainer(clusterId, role);
        if ( container != null ) {
            return DockerContainersUtil.getIpAddress( dockerClient, container.getId() );
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

        if (clusterId != null) {

            MarathonClient marathon = new MarathonClient( getContainerIp(clusterId, "marathon") );
            marathon.killAllApps();

            destroyContainers(clusterId);

            File minimesosFile = getMinimesosFile();
            if (minimesosFile.exists()) {
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

    public static File getMinimesosDir() {

        File hostDir = getMinimesosHostDir();
        File minimesosDir = new File( hostDir, ".minimesos");
        if( !minimesosDir.exists() ) {
            if( !minimesosDir.mkdirs() ) {
                throw new MinimesosException( "Failed to create " + minimesosDir.getAbsolutePath() + " directory" );
            }
        }

        return minimesosDir;
    }

    public static File getMinimesosHostDir() {
        String sp = System.getProperty(MINIMESOS_HOST_DIR_PROPERTY);
        if( sp == null ) {
            sp = System.getProperty("user.dir");
        }
        return new File( sp );
    }

    public void writeClusterId() {
        File minimesosDir = getMinimesosDir();
        try {
            FileUtils.forceMkdir(minimesosDir);
            Files.write(Paths.get(minimesosDir.getAbsolutePath() + "/" + MINIMESOS_FILE_PROPERTY), getClusterId().getBytes());
        } catch (IOException ie) {
            LOGGER.error("Could not write .minimesos folder", ie);
            throw new RuntimeException(ie);
        }
    }

    public void waitForState(final Predicate<State> predicate, int seconds) {
        Awaitility.await().atMost(seconds, TimeUnit.SECONDS).until(() -> {
            try {
                return predicate.test(State.fromJSON(MesosCluster.this.getStateInfoJSON().toString()));
            } catch (InternalServerErrorException e) {
                LOGGER.error(e);
                // This probably means that the mesos cluster isn't ready yet..
                return false;
            }
        });
    }

    public void waitForState(Predicate<State> predicate) {
        waitForState(predicate, 20);
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

    public static void printServiceUrl(String clusterId, String serviceName, MinimesosCliCommand cmd) {
        String dockerHostIp = System.getenv("DOCKER_HOST_IP");
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            for (String name : container.getNames()) {
                if (name.contains("minimesos-" + serviceName + "-" + clusterId)) {
                    String uri, ip;
                    if (!cmd.isExposedHostPorts() || dockerHostIp.isEmpty()) {
                        ip = DockerContainersUtil.getIpAddress( dockerClient, container.getId() );
                    } else {
                        ip = dockerHostIp;
                    }
                    switch (serviceName) {
                        case "master":
                            uri = "export MINIMESOS_MASTER=http://" + ip + ":" + MesosMaster.MESOS_MASTER_PORT;
                            break;
                        case "marathon":
                            uri = "export MINIMESOS_MARATHON=http://" + ip + ":" + Marathon.MARATHON_PORT;
                            break;
                        case "zookeeper":
                            uri = "export MINIMESOS_ZOOKEEPER=" + ZooKeeper.formatZKAddress(ip);
                            break;
                        case "consul":
                            uri = "export MINIMESOS_CONSUL=http://" + ip + ":" + Consul.DEFAULT_CONSUL_PORT + "\n" +
                            "export MINIMESOS_CONSUL_IP=" + ip;
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

    public static void deployMarathonApp(String clusterId, String marathonJson) {
        String marathonIp = getContainerIp(clusterId, "marathon");
        if (marathonIp == null) {
            throw new MinimesosException("Marathon container is not found in cluster " + MesosCluster.readClusterId());
        }

        MarathonClient marathonClient = new MarathonClient(marathonIp);
        LOGGER.debug(String.format("Installing %s app on marathon %s", marathonJson, marathonIp));

        marathonClient.deployApp(marathonJson);
    }

    public String getStateUrl() {
        return "http://" + getMesosMasterContainer().getIpAddress() + ":5050/state.json";
    }

}
