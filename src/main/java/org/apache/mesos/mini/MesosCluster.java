package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.util.DockerUtil;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

public class MesosCluster extends ExternalResource {

    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    public final DockerUtil dockerUtil;
    private final MesosContainer mesosContainer;
    final private MesosClusterConfig config;
    private final PrivateDockerRegistry privateDockerRegistry;
    private final DockerProxy dockerProxy;
    public DockerClient dockerClient;

    public MesosCluster(MesosClusterConfig config) {
        this.dockerUtil = new DockerUtil(config.dockerClient);
        this.config = config;
        this.dockerClient = config.dockerClient;
        mesosContainer = new MesosContainer(this.dockerClient, this.config);
        privateDockerRegistry = new PrivateDockerRegistry(this.dockerClient, this.config);
        dockerProxy = new DockerProxy(this.dockerClient);
    }

    public void start() {
        try {
            String proxyContainerId = dockerProxy.startProxy();

            // Pulls registry images and start container
            String registryContainerId = privateDockerRegistry.startPrivateRegistryContainer();

            // start the container
            String mesosLocalContainerId = mesosContainer.startMesosLocalContainer(registryContainerId);

            // wait until the given number of slaves are registered
            new MesosClusterStateResponse(mesosContainer.getMesosMasterURL(), config.numberOfSlaves).waitFor();

        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);
        }
    }

    public JSONObject getStateInfo() throws UnirestException {
        return Unirest.get("http://" + mesosContainer.mesosMasterIP + ":" + config.mesosMasterPort + "/state.json").asJson().getBody().getObject();
    }


    public String getMesosMasterURL(){
        return mesosContainer.getMesosMasterURL();
    }

    // For usage as JUnit rule...
    @Override
    protected void before() throws Throwable {
        start();
    }

}
