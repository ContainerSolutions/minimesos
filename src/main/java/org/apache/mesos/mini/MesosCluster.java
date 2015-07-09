package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosClusterConfig.ImageToBuild;
import org.apache.mesos.mini.state.State;
import org.apache.mesos.mini.util.DockerUtil;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class MesosCluster extends ExternalResource {

    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    public final DockerUtil dockerUtil;

    private ArrayList<String> containerIds = new ArrayList<String>();

    final private MesosClusterConfig config;

    private String mesosMasterIP;

    public DockerClient dockerClient;

    public MesosCluster(MesosClusterConfig config) {
        this.dockerUtil = new DockerUtil(config.dockerClient);
        this.config = config;
        this.dockerClient = config.dockerClient;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("Running shutdown hook");
                MesosCluster.this.stop();
            }
        });
    }

    public void start() {
        startProxy();

        // build required the images the test might have configured
        buildTestFixureImages();

        // Pulls registry images and start container
        // TODO start the registry only if we have at least one DinD-image to push
        String registryContainerId = startPrivateRegistryContainer();

        // push all docker in docker images with tag system tests to private registry
        pushDindImagesToPrivateRegistry();

        // start the container
        String mesosLocalContainerId = startMesosLocalContainer(registryContainerId);

        // determine mesos-master ip
        mesosMasterIP =  dockerUtil.getContainerIp(mesosLocalContainerId);

        // we have to pull the dind images and re-tag the images so they get their original name
        pullDindImagesAndRetagWithoutRepoAndLatestTag(mesosLocalContainerId);

        // wait until the given number of slaves are registered
        new MesosClusterStateResponse(getMesosMasterURL(), config.numberOfSlaves).waitFor();
    }

    private void buildTestFixureImages() {
        if (!System.getProperty("mesos.mini.build_images", "").equals("false")) {
            for (ImageToBuild image : config.imagesToBuild) {
                dockerUtil.buildImageFromFolder(image.srcFolder, image.tag);
            }
        }
    }

    private void startProxy() {
        // TODO allow disabling pull using system properties to save some development time
        dockerUtil.pullImage("paintedfox/tinyproxy", "latest");

        CreateContainerCmd command = dockerClient.createContainerCmd("paintedfox/tinyproxy")
                .withName("mini-mesos-proxy") // give the container a new so we can find it in the logs
                .withPortBindings(PortBinding.parse("0.0.0.0:8888:8888"));

        String containerId = dockerUtil.createAndStart(command);
        containerIds.add(containerId);

    }

    private void pullDindImagesAndRetagWithoutRepoAndLatestTag(String mesosClusterContainerId) {

        for (String image : config.dindImages) {

            try {
                Thread.sleep(2000); // we have to wait
            } catch (InterruptedException e) {
            }

            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(mesosClusterContainerId)
                    .withAttachStdout(true).withCmd("docker", "pull", "private-registry:5000/" + image + ":systemtest").exec();
            InputStream execCmdStream = dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec();
            assertThat(DockerUtil.consumeInputStream(execCmdStream), containsString("Download complete"));

            execCreateCmdResponse = dockerClient.execCreateCmd(mesosClusterContainerId)
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
        final String MESOS_LOCAL_IMAGE = "mesos-local";

        String mesosClusterContainerName = generateMesosMasterContainerName();

        dockerUtil.buildImageFromFolder(MESOS_LOCAL_IMAGE, MESOS_LOCAL_IMAGE);

        CreateContainerCmd command = dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE)
                .withName(mesosClusterContainerName)
                .withPrivileged(true)
                // the registry container will be known as 'private-registry' to mesos-local
                .withLinks(Link.parse(registryContainerName + ":private-registry"))
                .withEnv(createMesosLocalEnvironment())
                .withVolumes(new Volume("/sys/fs/cgroup"))
                .withBinds(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup:rw"));

        String containerId = dockerUtil.createAndStart(command);

        containerIds.add(containerId);

        return containerId;
    }

    private String[] createMesosLocalEnvironment() {
        ArrayList<String> envs = new ArrayList<String>();
        envs.add("NUMBER_OF_SLAVES=" + config.numberOfSlaves);
        envs.add("MESOS_QUORUM=1");
        envs.add("MESOS_ZK=zk://localhost:2181/mesos");
        envs.add("MESOS_EXECUTOR_REGISTRATION_TIMEOUT=5mins");
        envs.add("MESOS_CONTAINERIZERS=docker,mesos");
        envs.add("MESOS_ISOLATOR=cgroups/cpu,cgroups/mem");
        envs.add("MESOS_LOG_DIR=/var/log");
        for (int i = 1; i <= config.numberOfSlaves; i++){
            envs.add("SLAVE"+i+"_RESOURCES=" + config.slaveResources[i-1]);
        }
        return envs.toArray(new String[]{});
    }

    private String generateRegistryContainerName() {
        return "registry_" + new SecureRandom().nextInt();
    }

    private String generateMesosMasterContainerName() {
        return "mini_mesos_cluster_" + new SecureRandom().nextInt();
    }

    private File createRegistryStorageDirectory() {
        File registryStorageRootDir = new File(".registry");

        if (!registryStorageRootDir.exists()) {
            LOGGER.info("The private registry storage root directory doesn't exist, creating one...");
            registryStorageRootDir.mkdir();
        }
        return registryStorageRootDir;
    }

    private String startPrivateRegistryContainer() {
        final String REGISTRY_IMAGE_NAME = "registry";
        final String REGISTRY_TAG = "0.9.1";

        dockerUtil.pullImage(REGISTRY_IMAGE_NAME, REGISTRY_TAG);

        CreateContainerCmd command = dockerClient.createContainerCmd(REGISTRY_IMAGE_NAME + ":" + REGISTRY_TAG)
                .withName(generateRegistryContainerName())
                .withExposedPorts(ExposedPort.parse("5000"))
                .withEnv("STORAGE_PATH=/var/lib/registry")
                .withVolumes(new Volume("/var/lib/registry"))
                .withBinds(Bind.parse(createRegistryStorageDirectory().getAbsolutePath() + ":/var/lib/registry:rw"))
                .withPortBindings(PortBinding.parse("0.0.0.0:" + config.privateRegistryPort + ":5000"));

        String containerId = dockerUtil.createAndStart(command);

        containerIds.add(containerId);

        return containerId;
    }

    public void stop() {
        for (String containerId : containerIds) {
            try {
                dockerClient.removeContainerCmd(containerId).withForce().exec();
                LOGGER.info("Removing container " + containerId);
            } catch (Exception ignore){}
        }
    }

    public State getStateInfo() throws UnirestException {
        String json = Unirest.get("http://" + mesosMasterIP + ":" + config.mesosMasterPort + "/state.json").asString().getBody();

        return State.fromJSON(json);
    }

    public JSONObject getStateInfoJSON() throws UnirestException {

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

}
