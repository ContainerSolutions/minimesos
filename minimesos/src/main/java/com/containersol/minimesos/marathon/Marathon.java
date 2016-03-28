package com.containersol.minimesos.marathon;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.AppConfig;
import com.containersol.minimesos.config.MarathonConfig;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Marathon container. Marathon is a cluster-wide init and control system for services in cgroups or Docker containers.
 */
public class Marathon extends AbstractContainer {

    private static Logger LOGGER = LoggerFactory.getLogger(Marathon.class);

    private final MarathonConfig config;

    private ZooKeeper zooKeeper;

    public Marathon(DockerClient dockerClient, ZooKeeper zooKeeper) {
        this(dockerClient, zooKeeper, new MarathonConfig());
    }

    public Marathon(DockerClient dockerClient, ZooKeeper zooKeeper, MarathonConfig config) {
        super(dockerClient);
        this.zooKeeper = zooKeeper;
        this.config = config;
    }

    public Marathon(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        super(dockerClient, cluster, uuid, containerId);
        this.config = new MarathonConfig();
    }

    @Override
    public String getRole() {
        return "marathon";
    }

    @Override
    protected void pullImage() {
        pullImage(config.getImageName(), config.getImageTag());
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(MarathonConfig.MARATHON_PORT);
        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(exposedPort, Ports.Binding(MarathonConfig.MARATHON_PORT));
        }
        return dockerClient.createContainerCmd(config.getImageName() + ":" + config.getImageTag())
                .withName( getName() )
                .withExtraHosts("minimesos-zookeeper:" + this.zooKeeper.getIpAddress())
                .withCmd("--master", "zk://minimesos-zookeeper:2181/mesos", "--zk", "zk://minimesos-zookeeper:2181/marathon")
                .withExposedPorts(exposedPort)
                .withPortBindings(portBindings);
    }

    @Override
    public void start(int timeout) {
        super.start(timeout);

        List<AppConfig> apps = getConfig().getApps();
        for (AppConfig app : apps) {
            deployApp(app.getMarathonJsonFile());
        }
    }

    /**
     * Deploys a Marathon app via a file
     *
     * @param marathonJsonFile Marathon JSON file
     */
    public void deployApp(File marathonJsonFile) {
        LOGGER.debug("Deploying app from '" + marathonJsonFile.getAbsolutePath() + "'");
        try {
            String jsonString = FileUtils.readFileToString(marathonJsonFile);
            deployApp(jsonString);
        } catch (IOException e) {
            throw new MinimesosException("Could not read Marathon JSON file: '" +  marathonJsonFile.getAbsolutePath() + "'. " + e.getMessage());
        }
    }

    /**
     * Deploys a Marathon app by JSON string
     *
     * @param jsonString JSON string
     */
    public void deployApp(String jsonString) {
        String marathonEndpoint = getMarathonEndpoint();
        try {
            byte[] app = jsonString.getBytes(Charset.forName("UTF-8"));
            HttpResponse<JsonNode> response = Unirest.post(marathonEndpoint + "/v2/apps").header("accept", "application/json").body(app).asJson();
            JSONObject deployResponse = response.getBody().getObject();
            if (response.getStatus() == HttpStatus.SC_CREATED) {
                LOGGER.debug(deployResponse.toString());
            } else {
                throw new MinimesosException("Marathon did not accept the app: " + deployResponse);
            }
        } catch (UnirestException e) {
            String msg = "Could not deploy app on Marathon at " + marathonEndpoint + " => " + e.getMessage();
            LOGGER.error(msg);
            throw new MinimesosException(msg, e);
        }
        LOGGER.debug(String.format("Installing an app on marathon %s", getMarathonEndpoint()));
    }

    /**
     * Kill all apps that are currently running.
     */
    public void killAllApps() {
        String marathonEndpoint = getMarathonEndpoint();
        JSONObject appsResponse;
        try {
            appsResponse = Unirest.get(marathonEndpoint + "/v2/apps").header("accept", "application/json").asJson().getBody().getObject();
            if (appsResponse.length() == 0) {
                return;
            }
        } catch (UnirestException e) {
            LOGGER.error("Could not retrieve apps from Marathon at " + marathonEndpoint);
            return;
        }

        JSONArray apps = appsResponse.getJSONArray("apps");
        for (int i = 0; i < apps.length(); i++) {
            JSONObject app = apps.getJSONObject(i);
            String appId = app.getString("id");
            try {
                Unirest.delete(marathonEndpoint + "/v2/apps" + appId).asJson();
            } catch (UnirestException e) {
                LOGGER.error("Could not delete app " + appId + " at " + marathonEndpoint);
            }
        }
    }

    public String getMarathonEndpoint() {
        return "http://" + getIpAddress() + ":" + MarathonConfig.MARATHON_PORT;
    }

    public void waitFor() {
        LOGGER.debug("Waiting for Marathon to be ready");
        await("Marathon did not start responding").atMost(getCluster().getClusterConfig().getTimeout(), TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(new MarathonApiIsReady());
    }

    public MarathonConfig getConfig() {
        return config;
    }

    private class MarathonApiIsReady implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            try {
                Unirest.get(getMarathonEndpoint() + "/v2/apps").header("accept", "application/json").asJson().getBody().getObject();
            } catch (UnirestException e) {
                return false;
            }
            return true;
        }
    }
}
