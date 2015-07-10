package org.apache.mesos.mini;

import com.github.dockerjava.api.InternalServerErrorException;
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
import org.apache.mesos.mini.state.State;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.apache.mesos.mini.util.Predicate;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;

/**
 * Starts the mesos cluster. Responsible for setting up proxy and private docker registry. Once started, users can add
 * their own images to the private registry.
 */
public class MesosCluster extends ExternalResource {
    private static Logger LOGGER = Logger.getLogger(MesosCluster.class);
    private final MesosClusterConfig config;
    private MesosContainer mesosContainer;
    private DockerUtil dockerUtil;

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
        this.dockerUtil = new DockerUtil(config.dockerClient);
    }

    public void start() {
        try {
            DockerProxy dockerProxy = new DockerProxy(config.dockerClient);
            dockerProxy.start();

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
            dockerUtil.stop(); // remove all created docker containers (not handled by after then re-throwing e)
            throw e;
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

    public State getStateInfo() throws UnirestException {
        String json = Unirest.get("http://" + mesosContainer.getMesosMasterURL() + "/state.json").asString().getBody();

        return State.fromJSON(json);
    }

    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get("http://" + mesosContainer.getMesosMasterURL() + "/state.json").asJson().getBody().getObject();
    }

    public String getMesosMasterURL(){
        return mesosContainer.getMesosMasterURL();
    }

    public void waitForState(final Predicate<State> predicate, int seconds) {
        await().atMost(seconds, TimeUnit.SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    return predicate.test(MesosCluster.this.getStateInfo());
                }
                catch(InternalServerErrorException e) {
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

    @Override
    protected void before() throws Throwable {
            start();
    }

    @Override
    protected void after() {
        dockerUtil.stop();
    }
}
