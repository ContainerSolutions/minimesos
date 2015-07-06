package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.*;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

public class MesosCluster extends ExternalResource {

    static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    // TODO pull that docker image from Dockerhub -> take version which matches the docker host file storage e.g. aufs (still to create mesos-local-aufs and all other images)
    public String mesosLocalImage = "mesos-local";
    public String registryImage = "registry";

    private ArrayList<String> containerNames = new ArrayList<String>();

    final private MesosClusterConfig config;

    public DockerClient dockerClient;

    public CreateContainerResponse createMesosClusterContainerResponse;
    private CreateContainerResponse createRegistryContainerResponse;

    public StartContainerCmd startMesosClusterContainerCmd;
    private StartContainerCmd startRegistryContainerCmd;

    public String dockerHost;

    public MesosCluster(int numberOfSlaves, String slaveConfig, String[] dindImages) {
        this(MesosClusterConfig.builder().defaultDockerClient().numberOfSlaves(numberOfSlaves).slaveResources(slaveConfig).dockerInDockerImages(dindImages).build());
    }

    public MesosCluster(MesosClusterConfig config) {
        this.config = config;
        this.dockerClient = config.dockerClient;
    }


    public void start() {
        try {

            // Pulls registry images and start container
            String registryContainerName = startPrivateRegistryContainer();

            // push all docker in docker images with tag system tests to private registry
            pushDindImagesToPrivateRegistry();

            // builds the mesos-local image
            buildMesosLocalImage();

            // start the container
            startMesosLocalContainer(registryContainerName);

            // wait until the given number of slaves are registered
            assertMesosMasterStateCanBePulled(new MesosClusterStateResponse(config));
        } catch (Exception e) {
            LOGGER.error("Error during startup", e);
            stop(); // cleanup and remove started containers
        }
    }

    private void pushDindImagesToPrivateRegistry() {
        for (String image : config.dindImages) {
            String imageWithPrivateRepoName = "localhost:" + config.privateRegistryPort + "/" + image;
            LOGGER.debug("*****************************         Tagging image \"" + imageWithPrivateRepoName + "\"         *****************************");
            dockerClient.tagImageCmd(mesosLocalImage, imageWithPrivateRepoName, "systemtest").withForce(true).exec();
            LOGGER.debug("*****************************         Pushing image \"" + imageWithPrivateRepoName + ":systemtest\" to private registry        *****************************");
            InputStream responsePushImage = dockerClient.pushImageCmd(imageWithPrivateRepoName).withTag("systemtest").exec();

            assertThat(asString(responsePushImage), containsString("Image successfully pushed"));
        }
    }

    private void startMesosLocalContainer(String registryContainerName) {
        String mesosClusterContainerName = "mini_mesos_cluster_" + new SecureRandom().nextInt();
        LOGGER.debug("*****************************         Creating container \"" + mesosClusterContainerName + "\"         *****************************");

        createMesosClusterContainerResponse = dockerClient.createContainerCmd(mesosLocalImage)
                .withName(mesosClusterContainerName)
                .withExposedPorts(ExposedPort.parse(config.mesosMasterPort.toString()), ExposedPort.parse("2181"))
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.mesosMasterPort + ":" + config.mesosMasterPort), PortBinding.parse("0.0.0.0:2181:2181"))
                .withPrivileged(true)
                .withLinks(Link.parse(registryContainerName + ":private-registry"))
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


        containerNames.add(0, mesosClusterContainerName);


        startMesosClusterContainerCmd = dockerClient.startContainerCmd(createMesosClusterContainerResponse.getId());
        startMesosClusterContainerCmd.exec();
    }

    private void buildMesosLocalImage() {
        String fullLog;
        InputStream responseBuildImage = dockerClient.buildImageCmd(new File(Thread.currentThread().getContextClassLoader().getResource(mesosLocalImage).getFile())).withTag(mesosLocalImage).exec();

        fullLog = asString(responseBuildImage);
        assertThat(fullLog, containsString("Successfully built"));
    }

    private String startPrivateRegistryContainer() {
        String registryContainerName = "registry_" + new SecureRandom().nextInt();
        LOGGER.debug("*****************************         Pulling image \"" + registryImage + "\"         *****************************");
        InputStream responsePullImages = dockerClient.pullImageCmd(registryImage).exec();
        String fullLog = asString(responsePullImages);
        assertThat(fullLog, anyOf(containsString("Download complete"), containsString("Already exists")));


        LOGGER.debug("*****************************         Creating container \"" + registryContainerName + "\"         *****************************");
        createRegistryContainerResponse = dockerClient.createContainerCmd(registryImage)
                .withName(registryContainerName)
                .withExposedPorts(ExposedPort.parse("5000"))
                .withPortBindings(PortBinding.parse(config.privateRegistryPort + ":" + config.privateRegistryPort))
                .exec();

        containerNames.add(0, registryContainerName);

        startRegistryContainerCmd = dockerClient.startContainerCmd(createRegistryContainerResponse.getId());
        startRegistryContainerCmd.exec();
        return registryContainerName;
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


    protected String asString(InputStream response) {
        return consumeAsString(response);
    }


    public static String consumeAsString(InputStream response) {

        StringWriter logwriter = new StringWriter();

        try {
            LineIterator itr = IOUtils.lineIterator(response, "UTF-8");

            while (itr.hasNext()) {
                String line = itr.next();
                logwriter.write(line + (itr.hasNext() ? "\n" : ""));
                LOGGER.info(line);
            }
            response.close();

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(response);
        }
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
