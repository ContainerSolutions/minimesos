package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.*;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;

public class MesosCluster extends ExternalResource {

    static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    // TODO pull that docker image from Dockerhub -> take version which matches the docker host file storage e.g. aufs (still to create mesos-local-aufs and all other images)
    public String mesosLocalImage = "mesos-local";

    private ArrayList<String> containerNames = new ArrayList<String>();

    final private MesosClusterConfig config;

    public DockerClient dockerClient;

    public CreateContainerResponse createContainerResponse;

    public StartContainerCmd startContainerCmd;

    public String dockerHost;

    public MesosCluster(int numberOfSlaves, String slaveConfig) {
        this(MesosClusterConfig.builder().defaultDockerClient().numberOfSlaves(numberOfSlaves).slaveResources(slaveConfig).build());
    }

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
        this.dockerClient = config.dockerClient;
    }


    public void start() {
        String containerName = "mini_mesos_cluster_" + new SecureRandom().nextInt();
        LOGGER.debug("*****************************         Creating container \"" + containerName + "\"         *****************************");

        createContainerResponse = dockerClient.createContainerCmd(mesosLocalImage)
                .withName(containerName)
                .withExposedPorts(ExposedPort.parse(config.mesosMasterPort.toString()), ExposedPort.parse("2181"))
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.mesosMasterPort + ":" + config.mesosMasterPort), PortBinding.parse("0.0.0.0:2181:2181"))
                .withPrivileged(true)
                .withEnv("NUMBER_OF_SLAVES=" + config.numberOfSlaves,
                        "MESOS_QUORUM=1",
                        "MESOS_ZK=zk://localhost:2181/mesos",
                        "MESOS_EXECUTOR_REGISTRATION_TIMEOUT=5mins",
                        "MESOS_CONTAINERIZERS=docker,mesos",
                        "MESOS_ISOLATOR=cgroups/cpu,cgroups/mem",
                        "MESOS_LOG_DIR=/var/LOGGER",
                        "MESOS_RESOURCES=" + config.slaveResources) // could be made configurable...
                .withVolumes(new Volume("/var/lib/docker/aufs"),
                        new Volume("/var/lib/docker/btrfs")
                        , new Volume("/var/lib/docker/execdriver"),
                        new Volume("/var/lib/docker/graph"),
                        new Volume("/var/lib/docker/init"),
                        new Volume("/var/lib/docker/repositories-aufs"),
                        new Volume("/var/lib/docker/tmp"),
                        new Volume("/var/lib/docker/trust"),
                        new Volume("/var/lib/docker/vfs"),
                        new Volume("/var/lib/docker/volumes"))

                .withBinds(Bind.parse("/var/lib/docker/aufs:/var/lib/docker/aufs:rw"),
                        Bind.parse("/var/lib/docker/btrfs:/var/lib/docker/btrfs:rw"),
                        Bind.parse("/var/lib/docker/execdriver:/var/lib/docker/execdriver:rw"),
                        Bind.parse("/var/lib/docker/graph:/var/lib/docker/graph:rw"),
                        Bind.parse("/var/lib/docker/init:/var/lib/docker/init:rw"),
                        Bind.parse("/var/lib/docker/repositories-aufs:/var/lib/docker/repositories-aufs:rw"),
                        Bind.parse("/var/lib/docker/tmp:/var/lib/docker/tmp:rw"),
                        Bind.parse("/var/lib/docker/trust:/var/lib/docker/trust:rw"),
                        Bind.parse("/var/lib/docker/vfs:/var/lib/docker/vfs:rw"),
                        Bind.parse("/var/lib/docker/volumes:/var/lib/docker/volumes:rw"))
                .exec();


        containerNames.add(containerName);

        startContainerCmd = dockerClient.startContainerCmd(createContainerResponse.getId());
        startContainerCmd.exec();

        assertMesosMasterStateCanBePulled(new MesosClusterStateResponse(config));
    }

    public void stop() {
        for (String containerName : containerNames) {
            try {
                LOGGER.debug("*****************************         Removing container \"" + containerName + "\"         *****************************");

                dockerClient.removeContainerCmd(containerName).withForce().exec();
            } catch (DockerException ignore) {
                ignore.printStackTrace();
            }
        }
    }


    public JSONObject getStateInfo() throws UnirestException {

        return Unirest.get("http://" + config.dockerHost.getHost() + ":" + config.mesosMasterPort + "/state.json").asJson().getBody().getObject();
    }


    // For usage as JUnit rule...
    @Override
    protected void before() throws Throwable {
        start();
    }

    @Override
    protected void after() {
        stop();
    }


    private void assertMesosMasterStateCanBePulled(MesosClusterStateResponse mesosMasterStateResponse) {
        try {
            await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(mesosMasterStateResponse, is(true));
        } catch (ConditionTimeoutException e) {
            stop();
            fail("MesosMaster did not expose its state withing 5 minutes");
        }
        LOGGER.info("MesosMaster state discovered successfully");
    }

    private static class MesosClusterStateResponse implements Callable<Boolean> {


        private final MesosClusterConfig config;

        public MesosClusterStateResponse(MesosClusterConfig config) {
            this.config = config;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                int activated_slaves = Unirest.get("http://" + config.dockerHost.getHost() + ":" + config.mesosMasterPort + "/state.json").asJson().getBody().getObject().getInt("activated_slaves");
                if (!(activated_slaves == config.numberOfSlaves)) {
                    LOGGER.info("Waiting for " + config.numberOfSlaves + " activated slaves - current number of activated slaves: " + activated_slaves);
                    return false;
                }
            } catch (UnirestException e) {
                LOGGER.info("Polling MesosMaster state on host: \"" + config.dockerHost.getHost() + ":" + config.mesosMasterPort + "\"...");
                return false;
            } catch (Exception e) {
                LOGGER.error("An error occured while polling mesos master", e);
                return false;
            }
            return true;
        }
    }

}
