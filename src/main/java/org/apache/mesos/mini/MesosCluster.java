package org.apache.mesos.mini;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.PortBinding;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.docker.DockerProxy;
import org.apache.mesos.mini.docker.DockerUtil;
import org.apache.mesos.mini.docker.PrivateDockerRegistry;
import org.apache.mesos.mini.mesos.MesosClusterConfig;
import org.apache.mesos.mini.mesos.MesosContainer;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

/**
 * Starts the mesos cluster. Responsible for setting up proxy and private docker registry. Once started, users can add
 * their own images to the private registry.
 */
public class MesosCluster extends ExternalResource {
    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);
    private final MesosClusterConfig config;
    private MesosContainer mesosContainer;

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            DockerProxy dockerProxy = new DockerProxy(config.dockerClient);
            dockerProxy.startProxy();

            // Pulls registry images and start container
            PrivateDockerRegistry privateDockerRegistry = new PrivateDockerRegistry(config.dockerClient, this.config);
            privateDockerRegistry.startPrivateRegistryContainer();

            // start the container
            mesosContainer = new MesosContainer(config.dockerClient, this.config);
            mesosContainer.startMesosLocalContainer(privateDockerRegistry.getContainerId());

            // wait until the given number of slaves are registered
            new MesosClusterStateResponse(mesosContainer.getMesosMasterURL(), config.numberOfSlaves).waitFor();

        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);
        }
    }

    /**
     * Pull and start a docker image. This container will be destroyed when the Mesos cluster is shut down.
     *
     * @param containerName The name of the image
     * @return The id of the container
     */
    public String addAndStartContainer(String containerName) {
        DockerUtil dockerUtil = new DockerUtil(config.dockerClient);
        dockerUtil.pullImage(containerName, "latest");
        CreateContainerCmd command = config.dockerClient.createContainerCmd(containerName).withPortBindings(PortBinding.parse("0.0.0.0:80:80"));
        return dockerUtil.createAndStart(command);
    }

    public JSONObject getStateInfo() throws UnirestException {
        return Unirest.get("http://" + mesosContainer.getMesosMasterURL() + "/state.json").asJson().getBody().getObject();
    }

    public String getMesosMasterURL(){
        return mesosContainer.getMesosMasterURL();
    }
}
