package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.jayway.awaitility.core.ConditionTimeoutException;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.util.DockerUtil;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.InputStream;
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

    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);
    private final DockerUtil dockerUtil;

    public String mesosLocalImage = "mesos-local";
    public String registryImage = "registry";

    private ArrayList<String> containerNames = new ArrayList<String>();

    final private MesosClusterConfig config;

    public String mesosMasterIP;

    public DockerClient dockerClient;

    public CreateContainerResponse createMesosClusterContainerResponse;
    private CreateContainerResponse createRegistryContainerResponse;

    private StartContainerCmd startRegistryContainerCmd;


    public MesosCluster(MesosClusterConfig config) {
        this.dockerUtil = new DockerUtil(config);
        this.config = config;
        this.dockerClient = config.dockerClient;
    }


    public void start() {
        try {

            // Pulls registry images and start container
            // TODO start the registry only if we have at least one DinD-image to push
            String registryContainerName = startPrivateRegistryContainer();

            // push all docker in docker images with tag system tests to private registry
            pushDindImagesToPrivateRegistry();

            // builds the mesos-local image
            dockerUtil.buildImageFromFolder(mesosLocalImage, mesosLocalImage);

            // start the container
            startMesosLocalContainer(registryContainerName);

            // determine mesos-master ip
            mesosMasterIP =  dockerUtil.getContainerIp(createMesosClusterContainerResponse.getId());

            // we have to pull the dind images and re-tag the images so they get their original name
            pullDindImagesAndRetagWithoutRepoAndLatestTag();

            // wait until the given number of slaves are registered
            assertMesosMasterStateCanBePulled(new MesosClusterStateResponse(getMesosMasterURL(), config.numberOfSlaves));


        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);
            stop(); // cleanup and remove started containers
        }
    }


    private void pullDindImagesAndRetagWithoutRepoAndLatestTag() {

        for (String image : config.dindImages) {

            try {
                Thread.sleep(2000); // we have to wait
            } catch (InterruptedException e) {
            }

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(createMesosClusterContainerResponse.getId())
                    .withAttachStdout(true).withCmd("docker", "pull", "private-registry:5000/" + image + ":systemtest").exec();
            InputStream execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            assertThat(DockerUtil.consumeInputStream(execCmdStream), containsString("Download complete"));

            execCreateCmdResponse = dockerClient.execCreateCmd(createMesosClusterContainerResponse.getId())
                    .withAttachStdout(true).withCmd("docker", "tag", "private-registry:5000/" + image + ":systemtest", image + ":latest").exec();
            execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            DockerUtil.consumeInputStream(execCmdStream);
        }
    }

    private void pushDindImagesToPrivateRegistry() {
        for (String image : config.dindImages) {
            String imageWithPrivateRepoName =  "localhost:" + config.privateRegistryPort + "/" + image;
            LOGGER.debug("*****************************         Tagging image \"" + imageWithPrivateRepoName + "\"         *****************************");
            dockerClient.tagImageCmd(image, imageWithPrivateRepoName, "systemtest").withForce(true).exec();
            LOGGER.debug("*****************************         Pushing image \"" + imageWithPrivateRepoName + ":systemtest\" to private registry        *****************************");
            InputStream responsePushImage = dockerClient.pushImageCmd(imageWithPrivateRepoName).withTag("systemtest").exec();

            assertThat(DockerUtil.consumeInputStream(responsePushImage), containsString("The push refers to a repository"));
        }
    }

    private String startMesosLocalContainer(String registryContainerName) {
        String mesosClusterContainerName = "mini_mesos_cluster_" + new SecureRandom().nextInt();
        LOGGER.debug("*****************************         Creating container \"" + mesosClusterContainerName + "\"         *****************************");



        ArrayList<String> envs = new ArrayList<String>();
        envs.add("NUMBER_OF_SLAVES=" + config.numberOfSlaves);
        envs.add("MESOS_QUORUM=1");
        envs.add( "MESOS_ZK=zk://localhost:2181/mesos");
        envs.add("MESOS_EXECUTOR_REGISTRATION_TIMEOUT=5mins");
        envs.add("MESOS_CONTAINERIZERS=docker,mesos");
        envs.add("MESOS_ISOLATOR=cgroups/cpu,cgroups/mem");
        envs.add("MESOS_LOG_DIR=/var/log");
        for (int i = 1; i <= config.numberOfSlaves; i++){
            envs.add("SLAVE"+i+"_RESOURCES=" + config.slaveResources[i-1]);
        }

        createMesosClusterContainerResponse = dockerClient.createContainerCmd(mesosLocalImage)
                .withName(mesosClusterContainerName)
                .withPrivileged(true)
                .withLinks(Link.parse(registryContainerName + ":private-registry"))
                .withEnv(envs.toArray(new String[]{})) // TODO make list and parse that list
                .withVolumes(new Volume("/sys/fs/cgroup"))
                .withBinds(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup:rw"))
                .exec();


        containerNames.add(0, mesosClusterContainerName);


        StartContainerCmd startMesosClusterContainerCmd = dockerClient.startContainerCmd(createMesosClusterContainerResponse.getId());
        startMesosClusterContainerCmd.exec();


        try {
            await().atMost(100, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(new ContainerEchoRespone(dockerClient, createMesosClusterContainerResponse.getId()), is(true));
        } catch (ConditionTimeoutException e) {
            stop();
            fail("Mesos-cluster did not start up within the given time");
        }
        return createMesosClusterContainerResponse.getId();
    }

    private String startPrivateRegistryContainer() {
        String registryTag = "0.9.1";
        LOGGER.debug("*****************************         Pulling image \"" + registryImage + "\"         *****************************");

        // Step 1: pull image
        InputStream responsePullImages = dockerClient.pullImageCmd(registryImage).withTag(registryTag).exec();
        String fullLog = DockerUtil.consumeInputStream(responsePullImages);
        assertThat(fullLog, anyOf(containsString("Download complete"), containsString("Already exists")));


        // Step 2: Create, configure and run the container
        // We need to configure it by setting a private repository
        // so that subsequent docker pushes of DinD-images will be faster
        String registryContainerName = "registry_mini_mesos_" + new SecureRandom().nextInt(); // TODO refactor into "generateUniqueContainerName"
        File registryStorageRootDir = new File(".registry"); // TODO factor out into "createRegistryStorageDir"

        if (!registryStorageRootDir.exists()) {
            LOGGER.info("The private registry storage root directory doesn't exist, creating one...");
            registryStorageRootDir.mkdir();
        }
        LOGGER.debug("*****************************         Creating container \"" + registryContainerName + "\"         *****************************");
        createRegistryContainerResponse = dockerClient.createContainerCmd(registryImage + ":" + registryTag)
                .withName(registryContainerName)
                .withExposedPorts(ExposedPort.parse("5000"))
                .withEnv("STORAGE_PATH=/var/lib/registry")
                .withVolumes(new Volume("/var/lib/registry"))
                .withBinds(Bind.parse(registryStorageRootDir.getAbsolutePath() + ":/var/lib/registry:rw"))
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.privateRegistryPort + ":5000"))
                .exec();

        containerNames.add(0, registryContainerName);

        // TODO create a util function that creates and starts an image with a specified name and (awaits a response)
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

        return Unirest.get("http://" + mesosMasterIP + ":" + config.mesosMasterPort + "/state.json").asJson().getBody().getObject();
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


    private void assertMesosMasterStateCanBePulled(MesosClusterStateResponse mesosMasterStateResponse) {
        try {
            await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(mesosMasterStateResponse, is(true));
        } catch (ConditionTimeoutException e) {
            stop();
            fail("MesosMaster did not expose its state withing 20 sec");
        }
        LOGGER.info("MesosMaster state discovered successfully");
    }

    // TODO factor out into its own file?
    private static class MesosClusterStateResponse implements Callable<Boolean> {


        private final String mesosMasterUrl;
        private final int expectedNumberOfSlaves;

        public MesosClusterStateResponse(String mesosMasterUrl, int expectedNumberOfSlaves) {
            this.mesosMasterUrl = mesosMasterUrl;
            this.expectedNumberOfSlaves = expectedNumberOfSlaves;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                int activated_slaves = Unirest.get("http://" + mesosMasterUrl + "/state.json").asJson().getBody().getObject().getInt("activated_slaves");
                if (!(activated_slaves == expectedNumberOfSlaves)) {
                    LOGGER.info("Waiting for " + expectedNumberOfSlaves + " activated slaves - current number of activated slaves: " + activated_slaves);
                    return false;
                }
            } catch (UnirestException e) {
                LOGGER.info("Polling MesosMaster state on host: \"" + mesosMasterUrl + "\"...");
                return false;
            } catch (Exception e) {
                LOGGER.error("An error occured while polling mesos master", e);
                return false;
            }
            return true;
        }
    }

    // TODO factor out into its own file? (TYPO IN NAME)
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
                assertThat(DockerUtil.consumeInputStream(execCmdStream), containsString("hello-container"));
            } catch (Exception e) {
                LOGGER.error("An error occured while waiting for container to echo test message", e);
                return false;
            }
            return true;
        }
    }

}
