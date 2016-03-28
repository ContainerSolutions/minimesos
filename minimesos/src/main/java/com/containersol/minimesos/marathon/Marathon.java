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

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
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
        LOGGER.debug("Waiting for Marathon to be ready at " + getMarathonEndpoint());
        await("Marathon did not start responding").atMost(getCluster().getClusterConfig().getTimeout(), TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(new MarathonApiIsReady());
    }

    public MarathonConfig getConfig() {
        return config;
    }

    public void deployApp(AppConfig app) {
        if (app.getFile() != null) {
            deployApp(app.getFile());
        } else {
            URL url = app.getUrl();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(getInputStream(url)))) {
                StringBuilder jsonBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    jsonBuilder.append(inputLine);
                }
                in.close();
                deployApp(jsonBuilder.toString());
            } catch (IOException e) {
                throw new MinimesosException("Could not deploy Marathon application '" + app.getUrl() + "' :" + e.getCause());
            }
//
//            try {
//
//
//                } catch (MalformedURLException e) {
//                    throw new MinimesosException("Invalid Marathon JSON URL at: " + url);
//                } catch (IOException e) {
//                    throw new MinimesosException("Could not read Marathon JSON string from URL: " + url);
//                }
//            } else {
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.setRequestMethod("GET");
//
//                // uncomment this if you want to write output to this url
//                //connection.setDoOutput(true);
//
//                // give it 15 seconds to respond
//                connection.setReadTimeout(15*1000);
//                connection.connect();
//
//                // read the output from the server
//
//                stringBuilder = new StringBuilder();
//
//                String line = null;
//                while ((line = reader.readLine()) != null)
//                {
//                    stringBuilder.append(line + "\n");
//                }
//                return stringBuilder.toString();
//            }
        }
    }

    private InputStream getInputStream(URL url) throws IOException {
        switch (url.getProtocol()) {
            case "https":
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                return con.getInputStream();
            case "http":
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                return connection.getInputStream();
            default:
                throw new MinimesosException("Unsupported protocol '" + url.getProtocol() + "' in URL " + url);
        }
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
