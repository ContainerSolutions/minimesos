package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.log4j.Logger;
import com.containersol.minimesos.container.AbstractContainer;

import java.util.Map;
import java.util.TreeMap;

public class MesosMasterExtended extends MesosMaster {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosMasterExtended.class);

    private final String mesosMasterImage;

    public final String mesosImageTag;

    private final String clusterId;

    private final Map<String, String> extraEnvironmentVariables;

    public MesosMasterExtended(DockerClient dockerClient, String zkPath, String mesosMasterImage, String mesosImageTag, String clusterId, Map<String, String> extraEnvironmentVariables) {
        super(dockerClient, zkPath);
        this.clusterId = clusterId;
        this.mesosMasterImage = mesosMasterImage;
        this.mesosImageTag = mesosImageTag;
        this.extraEnvironmentVariables = extraEnvironmentVariables;
    }

    @Override
    public void start() {
        super.start();
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = getMesosEnvVars();

        envs.putAll(this.extraEnvironmentVariables);

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    @Override
    protected void pullImage() {
        pullImage(mesosMasterImage, mesosImageTag);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {

        return dockerClient.createContainerCmd(mesosMasterImage + ":" + mesosImageTag)
                .withName("minimesos-master-" + clusterId + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(5050))
                .withEnv(createMesosLocalEnvironment());

    }

    public int getDockerPort() {
        return DOCKER_PORT;
    }

    public DockerClient getOuterDockerClient() {
        return dockerClient;
    }

    public String getMesosMasterImage() {
        return mesosMasterImage;
    }

    public String getMesosImageTag() {
        return mesosImageTag;
    }

    public Map<String, String> getExtraEnvironmentVariables() {
        return this.extraEnvironmentVariables;
    }
}
