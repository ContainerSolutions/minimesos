package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.log4j.Logger;
import com.containersol.minimesos.container.AbstractContainer;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class MesosMaster extends AbstractContainer {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosMaster.class);

    private final String mesosMasterImage;

    public final String mesosImageTag;

    private final String zkUrl;

    private final String clusterId;

    private final Map<String, String> extraEnvironmentVariables;

    public MesosMaster(DockerClient dockerClient, String zkPath, String mesosMasterImage, String mesosImageTag, String clusterId, Map<String, String> extraEnvironmentVariables) {
        super(dockerClient);
        this.clusterId = clusterId;
        this.zkUrl = zkPath;
        this.mesosMasterImage = mesosMasterImage;
        this.mesosImageTag = mesosImageTag;
        this.extraEnvironmentVariables = extraEnvironmentVariables;
    }

    @Override
    public void start() {
        super.start();
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = new TreeMap<>();

        envs.put("MESOS_QUORUM", "1");
        envs.put("MESOS_ZK", zkUrl);
        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        envs.put("MESOS_WORK_DIR", "/tmp/mesos");

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
