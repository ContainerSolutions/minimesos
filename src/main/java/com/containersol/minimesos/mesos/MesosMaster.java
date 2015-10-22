package com.containersol.minimesos.mesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.log4j.Logger;

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

    private final Boolean exposedHostPort;

    public MesosMaster(DockerClient dockerClient, String zkPath, String mesosMasterImage, String mesosImageTag, String clusterId, Map<String, String> extraEnvironmentVariables, Boolean exposedHostPort) {
        super(dockerClient);
        this.clusterId = clusterId;
        this.zkUrl = zkPath;
        this.mesosMasterImage = mesosMasterImage;
        this.mesosImageTag = mesosImageTag;
        this.extraEnvironmentVariables = extraEnvironmentVariables;
        this.exposedHostPort = exposedHostPort;
    }

    @Override
    public void start() {
        super.start();
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String, String> envs = new TreeMap<>();

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
        ExposedPort tcp5050 = ExposedPort.tcp(5050);
        Ports portBindings = new Ports();
        if (exposedHostPort) {
            portBindings.bind(tcp5050, Ports.Binding(5050));
        }
        return dockerClient.createContainerCmd(mesosMasterImage + ":" + mesosImageTag)
                .withName("minimesos-master-" + clusterId + "-" + getRandomId())
                .withExposedPorts(tcp5050)
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
