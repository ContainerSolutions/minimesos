package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.*;
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
import static org.hamcrest.Matchers.notNullValue;

public class MesosCluster extends ExternalResource {

    static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    // TODO pull that docker image from Dockerhub -> take version which matches the docker host file storage e.g. aufs (still to create mesos-local-aufs and all other images)
    public String mesosLocalImage = "mesos-local";
    public String registryImage = "registry";

    private ArrayList<String> containerNames = new ArrayList<String>();

    final private MesosClusterConfig config;

    public String mesosMasterIP;

    public DockerClient dockerClient;

    public CreateContainerResponse createMesosClusterContainerResponse;
    private CreateContainerResponse createRegistryContainerResponse;

    public StartContainerCmd startMesosClusterContainerCmd;
    private StartContainerCmd startRegistryContainerCmd;


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

            // determine mesos-master ip
            mesosMasterIP = determineMesosMasterIp();

            // we have to pull the dind images and re-tag the images so they get their original name
            pullDindImagesAndRetagWithoutRepoAndLatestTag();

            // wait until the given number of slaves are registered
            assertMesosMasterStateCanBePulled(new MesosClusterStateResponse(config));

        } catch (Error e) {
            LOGGER.error("Error during startup", e);
            stop(); // cleanup and remove started containers
        }
    }

    private String determineMesosMasterIp() {
        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(createMesosClusterContainerResponse.getId()).exec();

        assertThat(inspectContainerResponse.getNetworkSettings().getIpAddress(), notNullValue());

        return inspectContainerResponse.getNetworkSettings().getIpAddress();
    }

    private void pullDindImagesAndRetagWithoutRepoAndLatestTag() {

        for (String image : config.dindImages) {

            try {
                Thread.sleep(2000); // we have to wait
            } catch (InterruptedException e) {
            }

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(createMesosClusterContainerResponse.getId())
                    .withAttachStdout(true).withCmd("docker", "pull", "private-registry:" + config.privateRegistryPort + "/" + image + ":systemtest").exec();
            InputStream execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            assertThat(asString(execCmdStream), containsString("Download complete"));

            execCreateCmdResponse = dockerClient.execCreateCmd(createMesosClusterContainerResponse.getId())
                    .withAttachStdout(true).withCmd("docker", "tag", "private-registry:" + config.privateRegistryPort + "/" + image + ":systemtest", image + ":latest").exec();
            execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            asString(execCmdStream);
        }
    }

    private void pushDindImagesToPrivateRegistry() {
        for (String image : config.dindImages) {
            String imageWithPrivateRepoName = "localhost:" + config.privateRegistryPort + "/" + image;
            LOGGER.debug("*****************************         Tagging image \"" + imageWithPrivateRepoName + "\"         *****************************");
            dockerClient.tagImageCmd(image, imageWithPrivateRepoName, "systemtest").withForce(true).exec();
            LOGGER.debug("*****************************         Pushing image \"" + imageWithPrivateRepoName + ":systemtest\" to private registry        *****************************");
            InputStream responsePushImage = dockerClient.pushImageCmd(imageWithPrivateRepoName).withTag("systemtest").exec();

            assertThat(asString(responsePushImage), containsString("The push refers to a repository"));
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
                        "MESOS_LOG_DIR=/var/log",
                        "MESOS_RESOURCES=" + config.slaveResources[0]) // TODO make list and parse that list
                .withVolumes(new Volume("/sys/fs/cgroup"))
                .withBinds(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup:rw"))
                .exec();


        containerNames.add(0, mesosClusterContainerName);


        startMesosClusterContainerCmd = dockerClient.startContainerCmd(createMesosClusterContainerResponse.getId());
        startMesosClusterContainerCmd.exec();


        try {
            await().atMost(100, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(new ContainerEchoRespone(dockerClient, createMesosClusterContainerResponse.getId()), is(true));
        } catch (ConditionTimeoutException e) {
            stop();
            fail("Mesos-cluster did not start up within the given time");
        }

    }

    private void buildMesosLocalImage() {
        String fullLog;
        InputStream responseBuildImage = dockerClient.buildImageCmd(new File(Thread.currentThread().getContextClassLoader().getResource(mesosLocalImage).getFile())).withTag(mesosLocalImage).exec();

        fullLog = asString(responseBuildImage);
        assertThat(fullLog, containsString("Successfully built"));
    }

    private String startPrivateRegistryContainer() {
        String registryTag = "0.9.1";
        String registryContainerName = "registry_" + new SecureRandom().nextInt();
        LOGGER.debug("*****************************         Pulling image \"" + registryImage + "\"         *****************************");
        InputStream responsePullImages = dockerClient.pullImageCmd(registryImage).withTag(registryTag).exec();
        String fullLog = asString(responsePullImages);
        assertThat(fullLog, anyOf(containsString("Download complete"), containsString("Already exists")));


        File registryStorageRootDir = new File(".registry");

        if (!registryStorageRootDir.exists()) {
            LOGGER.info("The private registry storage root directory doesn't exist, creating one...");
            registryStorageRootDir.mkdir();
        }
        LOGGER.debug("*****************************         Creating container \"" + registryContainerName + "\"         *****************************");
        createRegistryContainerResponse = dockerClient.createContainerCmd(registryImage + ":" + registryTag)
                .withName(registryContainerName)
                .withExposedPorts(ExposedPort.parse(config.privateRegistryPort.toString()))
                .withEnv("STORAGE_PATH=/var/lib/registry")
                .withVolumes(new Volume("/var/lib/registry"))
                .withBinds(Bind.parse(registryStorageRootDir.getAbsolutePath() + ":/var/lib/registry:rw"))
                .withPortBindings(PortBinding.parse(config.privateRegistryPort + ":" + config.privateRegistryPort))
                .exec();

        containerNames.add(0, registryContainerName);

        startRegistryContainerCmd = dockerClient.startContainerCmd(createRegistryContainerResponse.getId());
        startRegistryContainerCmd.exec();


        try {
            await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(new ContainerEchoRespone(dockerClient, createRegistryContainerResponse.getId()), is(true));
        } catch (ConditionTimeoutException e) {
            stop();
            fail("Registry did not start up within the given time");
        }

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


    public String getMesosMasterURL(){
        return mesosMasterIP + ":" + config.mesosMasterPort;
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
            fail("MesosMaster did not expose its state withing 10 sec");
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

    private static class ContainerEchoRespone implements Callable<Boolean> {


        private final String containerId;
        private final DockerClient dockerClient;

        public ContainerEchoRespone(DockerClient dockerClient, String containerId) {
            this.dockerClient = dockerClient;
            this.containerId = containerId;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                        .withAttachStdout(true).withCmd("echo", "hello-container").exec();
                InputStream execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
                assertThat(MesosCluster.consumeAsString(execCmdStream), containsString("hello-container"));
            } catch (Exception e) {
                LOGGER.error("An error occured while waiting for container to echo test message", e);
                return false;
            }
            return true;
        }
    }

}
