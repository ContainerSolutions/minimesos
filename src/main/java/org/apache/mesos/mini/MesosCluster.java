package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosClusterConfig.ImageToBuild;
import org.apache.mesos.mini.util.DockerUtil;
import org.apache.mesos.mini.util.MesosClusterStateResponse;
import org.json.JSONObject;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class MesosCluster extends ExternalResource {

    public static Logger LOGGER = Logger.getLogger(MesosCluster.class);

    public final DockerUtil dockerUtil;
    private final MesosContainer mesosContainer;
    final private MesosClusterConfig config;
    public DockerClient dockerClient;
    private ArrayList<String> containerIds = new ArrayList<String>();

    public MesosCluster(MesosClusterConfig config) {
        this.dockerUtil = new DockerUtil(config.dockerClient);
        this.config = config;
        this.dockerClient = config.dockerClient;
        mesosContainer = new MesosContainer(this.dockerClient, this.config);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOGGER.info("Running shutdown hook");
                MesosCluster.this.stop();
            }
        });
    }

    public void start() {
        try {
            startProxy();

            // build required the images the test might have configured
            buildTestFixureImages();

            // Pulls registry images and start container
            // TODO start the registry only if we have at least one DinD-image to push
            String registryContainerId = startPrivateRegistryContainer();

            // push all docker in docker images with tag system tests to private registry
            pushDindImagesToPrivateRegistry();

            // start the container
            String mesosLocalContainerId = mesosContainer.startMesosLocalContainer(registryContainerId);
            containerIds.add(mesosLocalContainerId);

            // determine mesos-master ip
            mesosContainer.mesosMasterIP = dockerUtil.getContainerIp(mesosLocalContainerId);

            // we have to pull the dind images and re-tag the images so they get their original name
            pullDindImagesAndRetagWithoutRepoAndLatestTag(mesosLocalContainerId);

            // wait until the given number of slaves are registered
            new MesosClusterStateResponse(mesosContainer.getMesosMasterURL(), config.numberOfSlaves).waitFor();


        } catch (Throwable e) {
            LOGGER.error("Error during startup", e);
        }
    }

    private void buildTestFixureImages() {
        for (ImageToBuild image : config.imagesToBuild) {
            dockerUtil.buildImageFromFolder(image.srcFolder,image.tag);
        }
    }

    private void startProxy() {
        dockerUtil.pullImage("paintedfox/tinyproxy", "latest");

        CreateContainerCmd command = dockerClient.createContainerCmd("paintedfox/tinyproxy").withPortBindings(PortBinding.parse("0.0.0.0:8888:8888"));

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

    private String generateRegistryContainerName() {
        return "registry_" + new SecureRandom().nextInt();
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
            dockerClient.removeContainerCmd(containerId).withForce().exec();
            LOGGER.info("Removing container " + containerId);
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
