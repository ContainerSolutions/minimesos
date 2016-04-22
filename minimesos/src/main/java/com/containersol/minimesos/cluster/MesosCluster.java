package com.containersol.minimesos.cluster;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.state.State;
import com.containersol.minimesos.util.Predicate;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotFoundException;
import com.jayway.awaitility.Awaitility;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Mesos cluster with lifecycle methods such as start, install, info, state, stop and destroy.
 */
public class MesosCluster {

    private static final Logger LOGGER = LoggerFactory.getLogger(MesosCluster.class);

    public static final String MINIMESOS_HOST_DIR_PROPERTY = "minimesos.host.dir";

    private String clusterId;

    private final ClusterConfig clusterConfig;

    private List<ClusterProcess> memberPocesses = Collections.synchronizedList(new ArrayList<>());

    private boolean running = false;

    /**
     * Create a new MesosCluster with a specified cluster architecture.
     */
    public MesosCluster(ClusterConfig clusterConfig, List<ClusterProcess> processes) {
        this.memberPocesses = processes;
        this.clusterConfig = clusterConfig;

        clusterId = Integer.toUnsignedString(new SecureRandom().nextInt());
        for (ClusterProcess process : processes) {
            process.setCluster(this);
        }
    }

    /**
     * Recreate a MesosCluster object based on an existing cluster ID.
     *
     * @param clusterId the cluster ID of the cluster that is already running
     */
    public static MesosCluster loadCluster(String clusterId, MesosClusterFactory factory) {
        return new MesosCluster(clusterId, factory);
    }

    /**
     * This constructor is used for deserialization of running cluster
     *
     * @param clusterId ID of the cluster to deserialize
     */
    private MesosCluster(String clusterId, MesosClusterFactory factory) {
        this.clusterId = clusterId;
        this.clusterConfig = new ClusterConfig();

        factory.loadRunningCluster(this);

        if (memberPocesses.isEmpty()) {
            throw new MinimesosException("No containers found for cluster ID " + clusterId);
        }

        ZooKeeper zookeeper = getZooKeeper();
        MesosMaster master = getMaster();

        if (master != null && zookeeper != null) {
            for (MesosAgent mesosAgent : getAgents()) {
                mesosAgent.setZooKeeper(zookeeper);
            }

            master.setZooKeeper(zookeeper);

            if (getMarathon() != null) {
                getMarathon().setZooKeeper(zookeeper);
            }
        }

        running = true;
    }

    /**
     * Starts the Mesos cluster and its containers with 60 second timeout.
     * The method is used by frameworks
     */
    public void start() {
        start(clusterConfig.getTimeout());
    }

    /**
     * Starts the Mesos cluster and its containers with given timeout.
     *
     * @param timeoutSeconds seconds to wait until timeout
     */
    public void start(int timeoutSeconds) {
        if (running) {
            throw new IllegalStateException("Cluster " + clusterId + " is already running");
        }

        LOGGER.debug("Cluster " + getClusterId() + " - start");
        this.memberPocesses.forEach((container) -> container.start(timeoutSeconds));
        // wait until the given number of agents are registered
        getMaster().waitFor();

        Marathon marathon = getMarathon();
        if (marathon != null) {
            marathon.installMarathonApps();
        }

        running = true;
    }

    /**
     * Prints the state of the Mesos master or agent
     */
    public void state(PrintStream out, String agentContainerId) {
        JSONObject stateInfo;
        if (StringUtils.isEmpty(agentContainerId)) {
            stateInfo = getClusterStateInfo();
        } else {
            stateInfo = getAgentStateInfo(agentContainerId);
        }

        if (stateInfo != null) {
            out.println(stateInfo.toString(2));
        } else {
            throw new MinimesosException("Did not find the cluster or requested container");
        }
    }

    /**
     * Installs a Marathon app
     *
     * @param marathonJson JSON representation of Marathon app
     */
    public void install(String marathonJson) {
        if (marathonJson == null) {
            throw new MinimesosException("Specify a Marathon JSON app definition");
        }

        Marathon marathon = getMarathon();
        if (marathon == null) {
            throw new MinimesosException("Marathon container is not found in cluster " + clusterId);
        }

        String marathonIp = marathon.getIpAddress();
        LOGGER.debug(String.format("Installing %s app on marathon %s", marathonJson, marathonIp));

        marathon.deployApp(marathonJson);
    }

    /**
     * Destroys the Mesos cluster and its containers
     */
    public void destroy(MesosClusterFactory factory) {

        LOGGER.debug("Cluster " + getClusterId() + " - destroy");

        // stop applications, which are installed through marathon
        Marathon marathon = getMarathon();
        if (marathon != null) {
            marathon.killAllApps();
        }

        if (memberPocesses.size() > 0) {
            for (int i = memberPocesses.size() - 1; i >= 0; i--) {
                ClusterProcess container = memberPocesses.get(i);
                LOGGER.debug("Removing container [" + container.getContainerId() + "]");
                try {
                    container.remove();
                } catch (NotFoundException e) {
                    LOGGER.error(String.format("Cannot remove container %s, maybe it's already dead?", container.getContainerId()));
                }
            }
        }
        this.running = false;
        this.memberPocesses.clear();

        if (clusterId != null) {
            factory.destroyRunningCluster(clusterId);

            File sandboxLocation = new File(getHostDir(), ".minimesos/sandbox-" + clusterId);
            if (sandboxLocation.exists()) {
                try {
                    FileUtils.forceDelete(sandboxLocation);
                } catch (IOException e) {
                    String msg = String.format("Failed to force delete the cluster sandbox at %s", sandboxLocation.getAbsolutePath());
                    throw new MinimesosException(msg, e);
                }
            }

        } else {
            LOGGER.info("Minimesos cluster is not running");
        }

        this.running = false;

    }

    /**
     * Starts a container. This container will be removed when the Mesos cluster is shut down.
     *
     * @param process container to be started
     * @param timeout in seconds
     * @return container ID
     */
    public String addAndStartProcess(ClusterProcess process, int timeout) {
        process.setCluster(this);
        memberPocesses.add(process);

        LOGGER.debug(String.format("Starting %s (%s) container", process.getName(), process.getContainerId()));

        try {
            process.start(timeout);
        } catch (Exception exc) {
            String msg = String.format("Failed to start %s (%s) container", process.getName(), process.getContainerId());
            LOGGER.error(msg, exc);
            throw new MinimesosException(msg, exc);
        }

        return process.getContainerId();
    }

    /**
     * Starts a container. This container will be removed when the Mesos cluster is shut down.
     * The method is used by frameworks
     *
     * @param clusterProcess container to be started
     * @return container ID
     */
    public String addAndStartProcess(ClusterProcess clusterProcess) {
        return addAndStartProcess(clusterProcess, clusterConfig.getTimeout());
    }

    /**
     * Retrieves JSON with Mesos Cluster master state
     *
     * @return stage JSON
     */
    public JSONObject getClusterStateInfo() {
        try {
            return getMaster().getStateInfoJSON();
        } catch (UnirestException e) {
            throw new MinimesosException("Failed to retrieve state from Mesos Master", e);
        }
    }

    /**
     * Retrieves JSON with Mesos state of the given container
     *
     * @param containerId ID of the container to get state from
     * @return stage JSON
     */
    public JSONObject getAgentStateInfo(String containerId) {
        MesosAgent theAgent = null;
        for (MesosAgent agent : getAgents()) {
            if (agent.getContainerId().startsWith(containerId)) {
                if (theAgent == null) {
                    theAgent = agent;
                } else {
                    throw new MinimesosException("Provided ID " + containerId + " is not enough to uniquely identify container");
                }
            }
        }

        try {
            return (theAgent != null) ? theAgent.getStateInfoJSON() : null;
        } catch (UnirestException e) {
            throw new MinimesosException("Failed to retrieve state from Mesos Agent container " + theAgent.getContainerId(), e);
        }
    }

    public List<ClusterProcess> getMemberProcesses() {
        return memberPocesses;
    }

    public List<MesosAgent> getAgents() {
        return memberPocesses.stream().filter(Filter.mesosAgent()).map(c -> (MesosAgent) c).collect(Collectors.toList());
    }

    public MesosMaster getMaster() {
        Optional<MesosMaster> master = getOne(Filter.mesosMaster());
        return master.isPresent() ? master.get() : null;
    }

    public ZooKeeper getZooKeeper() {
        Optional<ZooKeeper> zooKeeper = getOne(Filter.zooKeeper());
        return zooKeeper.isPresent() ? zooKeeper.get() : null;
    }

    public Marathon getMarathon() {
        Optional<Marathon> marathon = getOne(Filter.marathon());
        return marathon.isPresent() ? marathon.get() : null;
    }

    public Consul getConsul() {
        Optional<Consul> container = getOne(Filter.consul());
        return container.isPresent() ? container.get() : null;
    }

    /**
     * Optionally get one of a certain type of type T. Note, this cast will always work because we are filtering on that type.
     * If it doesn't find that type, the optional is empty so the cast doesn't need to be performed.
     *
     * @param filter A predicate that is true when an {@link ClusterProcess} in the list is of type T
     * @param <T>    A container of type T that extends {@link ClusterProcess}
     * @return the first container it comes across.
     */
    @SuppressWarnings("unchecked")
    public <T extends ClusterProcess> Optional<T> getOne(java.util.function.Predicate<ClusterProcess> filter) {
        return (Optional<T>) getMemberProcesses().stream().filter(filter).findFirst();
    }

    public String getClusterId() {
        return clusterId;
    }

    public boolean isExposedHostPorts() {
        return clusterConfig.getExposePorts();
    }

    public boolean getMapAgentSandboxVolume() {
        return clusterConfig.getMapAgentSandboxVolume();
    }

    public void setExposedHostPorts(boolean exposedHostPorts) {
        clusterConfig.setExposePorts(exposedHostPorts);
    }

    public void waitForState(final Predicate<State> predicate) {
        Awaitility.await().atMost(clusterConfig.getTimeout(), TimeUnit.SECONDS).until(() -> {
            try {
                return predicate.test(State.fromJSON(getMaster().getStateInfoJSON().toString()));
            } catch (InternalServerErrorException e) { //NOSONAR
                // This means that the mesos cluster isn't ready yet..
                return false;
            }
        });
    }

    /**
     * Returns current user directory, which is mapped to host
     *
     * @return container directory, which is mapped to current directory on host
     */
    public static File getHostDir() {
        String sp = System.getProperty(MINIMESOS_HOST_DIR_PROPERTY);
        if (sp == null) {
            sp = System.getProperty("user.dir");
        }
        return new File(sp);
    }

    /**
     * Taking either URI or path to a file, returns string with its content
     *
     * @param location either absolute URI or path to a file
     * @return input stream with location content or null
     */
    public static InputStream getInputStream(String location) {
        InputStream is = null;

        if (location != null) {
            URI uri;
            try {
                uri = URI.create(location);
                if (!uri.isAbsolute()) {
                    uri = null;
                }
            } catch (IllegalArgumentException ignored) { //NOSONAR
                // means this is not a valid URI, could be filepath
                uri = null;
            }

            if (uri != null) {

                try {
                    is = uri.toURL().openStream();
                } catch (IOException e) {
                    throw new MinimesosException("Failed to open " + location + " as URL", e);
                }

            } else {
                // location is not an absolute URI, therefore treat it as relative or absolute path
                File file = new File(location);
                if (!file.exists()) {
                    file = new File(getHostDir(), location);
                }

                if (file.exists()) {
                    try {
                        is = new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new MinimesosException("Failed to open " + file.getAbsolutePath() + " file", e);
                    }
                }
            }
        }

        return is;
    }

    /**
     * @return configured or default logging level of all Mesos containers in the cluster
     */
    public String getLoggingLevel() {
        return clusterConfig.getLoggingLevel();
    }

    /**
     * @return configured or default Mesos version of all Mesos containers in the cluster
     */
    public String getMesosVersion() {
        return clusterConfig.getMesosVersion();
    }

    /**
     * @return either configured or composed with ID cluster name
     */
    public String getClusterName() {
        String name = clusterConfig.getClusterName();
        if (StringUtils.isBlank(name)) {
            name = "minimesos-" + clusterId;
        }
        return name;
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MesosCluster cluster = (MesosCluster) o;

        return clusterId.equals(cluster.clusterId);
    }

    @Override
    public int hashCode() {
        // logic of hashCode() has to match logic of equals()
        return clusterId.hashCode();
    }

    @Override
    public String toString() {
        return "MesosCluster{" +
                "clusterId='" + clusterId + '\'' +
                ", processes=" + memberPocesses +
                '}';
    }

}
