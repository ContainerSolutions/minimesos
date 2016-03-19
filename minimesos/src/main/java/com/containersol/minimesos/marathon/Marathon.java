package com.containersol.minimesos.marathon;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
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
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Marathon container. Marathon is a cluster-wide init and control system for services in cgroups or Docker containers.
 */
public class Marathon extends AbstractContainer {

    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);

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
                .withCmd("--master", getCluster().getZkContainer().getFormattedZKAddress() + "/mesos", "--zk", getCluster().getZkContainer().getFormattedZKAddress() + "/marathon")
                .withExposedPorts(exposedPort)
                .withNetworkMode(config.getNetworkMode())
                .withPortBindings(portBindings);
    }

    /**
     * Deploy a Marathon app or a framework
     *
     * @param appJson JSON string
     */
    public void deployApp(String appJson) {
        String marathonEndpoint = getMarathonEndpoint();
        try {
            byte[] app = appJson.getBytes(Charset.forName("UTF-8"));

            HttpResponse<JsonNode> response = Unirest.post(marathonEndpoint + "/v2/apps").header("accept", "application/json").body(app).asJson();
            JSONObject deployResponse = response.getBody().getObject();

            if (response.getStatus() == HttpStatus.SC_CREATED) {
                LOGGER.debug(deployResponse);
            } else {
                throw new MinimesosException("Marathon did not accept the app: " + deployResponse);
            }

        } catch (UnirestException e) {
            String msg = "Could not deploy app on Marathon at " + marathonEndpoint + " => " + e.getMessage();
            LOGGER.error(msg);
            throw new MinimesosException(msg, e);
        }
        LOGGER.info(String.format("Installing an app on marathon %s", getMarathonEndpoint()));
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
        await("Marathon did not start responding").atMost(getCluster().getClusterConfig().getTimeout(), TimeUnit.SECONDS).until(this::isReady);
    }

    public boolean isReady() {
        JSONObject appsResponse;
        try {
            appsResponse = Unirest.get(getMarathonEndpoint() + "/v2/apps").header("accept", "application/json").asJson().getBody().getObject();
            if (appsResponse.length() == 0) {
                return false;
            }
        } catch (UnirestException e) {
            return false;
        }

        return true;
    }
}
