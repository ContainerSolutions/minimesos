package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

import java.util.Map;
import java.util.TreeMap;

public class MesosMasterExtended extends MesosMaster {

    private static final int DOCKER_PORT = 2376;

    private final String mesosMasterImage;

    public final String mesosImageTag;

    private final String clusterId;

    private final Map<String, String> extraEnvironmentVariables;

    private final Boolean exposedHostPort;

    public MesosMasterExtended(DockerClient dockerClient, ZooKeeper zooKeeperContainer, String mesosMasterImage, String mesosImageTag, String clusterId, Map<String, String> extraEnvironmentVariables, Boolean exposedHostPort) {
        super(dockerClient, zooKeeperContainer);
        this.clusterId = clusterId;
        this.mesosMasterImage = mesosMasterImage;
        this.mesosImageTag = mesosImageTag;
        this.extraEnvironmentVariables = extraEnvironmentVariables;
        this.exposedHostPort = exposedHostPort;
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = getDefaultEnvVars();

        envs.putAll(this.extraEnvironmentVariables);
        envs.putAll(getSharedEnvVars());
        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    @Override
    protected void pullImage() {
        pullImage(mesosMasterImage, mesosImageTag);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ExposedPort exposedPort = ExposedPort.tcp(MESOS_MASTER_PORT);
        Ports portBindings = new Ports();
        if (exposedHostPort) {
            portBindings.bind(exposedPort, Ports.Binding(MESOS_MASTER_PORT));
        }
        return dockerClient.createContainerCmd(mesosMasterImage + ":" + mesosImageTag)
                .withName("minimesos-master-" + clusterId + "-" + getRandomId())
                .withEnv(createMesosLocalEnvironment())
                .withPortBindings(portBindings);
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
