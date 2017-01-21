package com.containersol.minimesos.marathon;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterUtil;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.AppConfig;
import com.containersol.minimesos.config.MarathonConfig;
import com.containersol.minimesos.integrationtest.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.util.Environment;
import com.containersol.minimesos.util.CollectionsUtils;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.utils.MarathonException;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.containersol.minimesos.config.MarathonConfig.*;
import static com.jayway.awaitility.Awaitility.await;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Marathon container. Marathon is a cluster-wide init and control system for services in cgroups or Docker containers.
 */
public class MarathonContainer extends AbstractContainer implements Marathon {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonContainer.class);

    private static final String TOKEN_HOST_DIR = "MINIMESOS_HOST_DIR";

    private static final String END_POINT_EXT = "/v2/apps";
    private static final String HEADER_ACCEPT = "accept";

    private final MarathonConfig config;

    private ZooKeeper zooKeeper;

    public MarathonContainer(MarathonConfig config) {
        super(config);
        this.config = config;
    }

    public MarathonContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new MarathonConfig());
    }

    public MarathonContainer(MesosCluster cluster, String uuid, String containerId, MarathonConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    @Override
    public String getRole() {
        return "marathon";
    }

    @Override
    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    public URI getServiceUrl() {
        URI serviceUri = null;

        String protocol = getServiceProtocol();

        String host;
        if (Environment.isRunningInJvmOnMacOsX()) {
            host = "localhost";
        } else {
            host = getIpAddress();
        }

        int port = getServicePort();
        String path = getServicePath();

        if (StringUtils.isNotEmpty(host)) {
            try {
                serviceUri = new URI(protocol, null, host, port, path, null, null);
            } catch (URISyntaxException e) {
                throw new MinimesosException("Failed to form service URL for " + getName(), e);
            }
        }

        return serviceUri;
    }


    @Override
    public void deleteApp(String app) {
        try {
            HttpResponse<JsonNode> deleteResponse = Unirest.delete(getServiceUrl().toString() + END_POINT_EXT + "/" + app).header(HEADER_ACCEPT, APPLICATION_JSON).asJson();
            deleteResponse.getBody().getObject();
            if (!(deleteResponse.getStatus() == HttpStatus.SC_OK)) {
                throw new MinimesosException("Could not delete app: " + app);
            }
        } catch (UnirestException e) {
            throw new MinimesosException("Could not delete app: " + app, e);
        }
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(MARATHON_PORT);
        Ports portBindings = new Ports();
        if (getCluster().isMapPortsToHost()) {
            portBindings.bind(exposedPort, Ports.Binding.bindPort(MARATHON_PORT));
        }
        return DockerClientFactory.build().createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName(getName())
                .withExtraHosts("minimesos-zookeeper:" + this.zooKeeper.getIpAddress())
                .withCmd(CollectionsUtils.splitCmd(config.getCmd()))
                .withExposedPorts(exposedPort)
                .withPortBindings(portBindings);
    }

    /**
     * Returns a Marathon endpoint
     *
     * @return String endpoint
     */
    private String getMarathonEndpoint() {
        return getServiceUrl().toString();
    }

    /**
     * Deploys a Marathon app by JSON string
     *
     * @param marathonJson JSON string
     */
    @Override
    public void deployApp(String marathonJson) {
        mesosphere.marathon.client.Marathon marathon = MarathonClient.getInstance(getMarathonEndpoint());

        try {
            marathon.createApp(constructApp(marathonJson));
        } catch (MarathonException e) {
            throw new MinimesosException("Marathon did not accept the app, error: " + e.toString());
        }

        LOGGER.debug(String.format("Installing an app on marathon %s", getMarathonEndpoint()));
    }

    /**
     * Updates a Marathon app by JSON string
     *
     * @param marathonJson JSON string
     */
    @Override
    public void updateApp(String marathonJson) {
        mesosphere.marathon.client.Marathon marathon = MarathonClient.getInstance(getMarathonEndpoint());

        try {
            App app = constructApp(marathonJson);
            boolean force = true;
            marathon.updateApp(app.getId(), app, force);
        } catch (MarathonException e) {
            throw new MinimesosException("Marathon could not update the app, error: " + e.toString());
        }

        LOGGER.debug(String.format("Installing an app on marathon %s", getMarathonEndpoint()));
    }

    /**
     * Return App given a JSON string
     *
     * @param appJson JSON string
     * @return App object
     */
    private App constructApp(String appJson) {
        Gson gson = new Gson();
        App appObject = gson.fromJson(replaceTokens(appJson), App.class);

        return appObject;
    }

    /**
     * Replaces ${MINIMESOS_[ROLE]}, ${MINIMESOS_[ROLE]_IP} and ${MINIMESOS_[ROLE]_PORT} tokens in the given string with actual values.
     * Also supports ${NETWORK_GATEWAY}
     *
     * @param source string to replace values in
     * @return updated string
     */
    public String replaceTokens(String source) {
        MesosCluster cluster = getCluster();
        // received JSON might contain tokens, which should be replaced before the installation
        List<ClusterProcess> uniqueRoles = ClusterUtil.getDistinctRoleProcesses(cluster.getMemberProcesses());
        String updatedJson = source;
        for (ClusterProcess process : uniqueRoles) {
            URI serviceUri = process.getServiceUrl();
            if (serviceUri != null) {
                updatedJson = replaceToken(updatedJson, MesosCluster.MINIMESOS_TOKEN_PREFIX + process.getRole().toUpperCase(), serviceUri.toString());
                updatedJson = replaceToken(updatedJson, MesosCluster.MINIMESOS_TOKEN_PREFIX + process.getRole().toUpperCase() + "_IP", serviceUri.getHost());
                updatedJson = replaceToken(updatedJson, MesosCluster.MINIMESOS_TOKEN_PREFIX + process.getRole().toUpperCase() + "_PORT", Integer.toString(serviceUri.getPort()));
            }
        }

        // replace independent from roles tokens
        String masterContainer = cluster.getMaster().getContainerId();
        updatedJson = replaceToken(updatedJson, MesosCluster.TOKEN_NETWORK_GATEWAY, DockerContainersUtil.getGatewayIpAddress(masterContainer));
        updatedJson = replaceToken(updatedJson, TOKEN_HOST_DIR, MesosCluster.getHostDir().getAbsolutePath());

        return updatedJson;
    }

    private static String replaceToken(String input, String token, String value) {
        String tokenRegex = String.format("\\$\\{%s\\}", token);
        return input.replaceAll(tokenRegex, value);
    }

    /**
     * Kill all apps that are currently running.
     */
    @Override
    public void killAllApps() {
        String marathonEndpoint = getServiceUrl().toString();
        JSONObject appsResponse;
        try {
            appsResponse = Unirest.get(marathonEndpoint + END_POINT_EXT).header(HEADER_ACCEPT, APPLICATION_JSON).asJson().getBody().getObject();
            if (appsResponse.length() == 0) {
                return;
            }
        } catch (UnirestException e) {
            throw new MinimesosException("Could not retrieve apps from Marathon at " + marathonEndpoint, e);
        }

        JSONArray apps = appsResponse.getJSONArray("apps");
        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.getJSONObject(i);
            String appId = app.getString("id");
            try {
                Unirest.delete(marathonEndpoint + END_POINT_EXT + appId).asJson();
            } catch (UnirestException e) { //NOSONAR
                // failed to delete one app; continue with others
                LOGGER.error("Could not delete app " + appId + " at " + marathonEndpoint, e);
            }
        }
    }

    @Override
    protected int getServicePort() {
        return MARATHON_PORT;
    }

    public void waitFor() {
        LOGGER.debug("Waiting for Marathon to be ready at " + getServiceUrl().toString());
        await("Marathon did not start responding").atMost(getCluster().getClusterConfig().getTimeout(), TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(new MarathonApiIsReady());
    }

    public MarathonConfig getConfig() {
        return config;
    }

    private class MarathonApiIsReady implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            try {
                Unirest.get(getServiceUrl().toString() + END_POINT_EXT).header(HEADER_ACCEPT, APPLICATION_JSON).asJson().getBody().getObject();
            } catch (UnirestException e) { //NOSONAR
                // meaning API is not ready
                return false;
            }
            return true;
        }
    }

    /**
     * If Marathon configuration requires, installs the applications
     */
    @Override
    public void installMarathonApps() {

        waitFor();

        List<AppConfig> apps = getConfig().getApps();
        for (AppConfig app : apps) {
            try {

                InputStream json = MesosCluster.getInputStream(app.getMarathonJson());
                if (json != null) {
                    deployApp(IOUtils.toString(json, "UTF-8"));
                } else {
                    throw new MinimesosException("Failed to find content of " + app.getMarathonJson());
                }

            } catch (IOException ioe) {
                throw new MinimesosException("Failed to load JSON from " + app.getMarathonJson(), ioe);
            }
        }

    }
}
