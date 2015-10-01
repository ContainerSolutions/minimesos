package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.log4j.Logger;
import com.containersol.minimesos.container.AbstractContainer;

import java.io.File;
import java.security.SecureRandom;
import java.util.TreeMap;

public class MesosMaster extends AbstractContainer {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosMaster.class);

    private final String mesosLocalImage;
    public final String registryTag;

    private final String zkUrl;

    public MesosMaster(DockerClient dockerClient, String zkPath, String mesosLocalImage, String registryTag) {
        super(dockerClient);
        this.zkUrl = zkPath;
        this.mesosLocalImage = mesosLocalImage;
        this.registryTag = registryTag;
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

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    String generateMesosMasterContainerName() {
        return "mini_mesos_cluster_" + new SecureRandom().nextInt();
    }

    @Override
    protected void pullImage() {
        pullImage(mesosLocalImage, registryTag);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        String mesosClusterContainerName = generateMesosMasterContainerName();
        String dockerBin = "/usr/bin/docker";

        if (! (new File(dockerBin).exists())) {
            dockerBin = "/usr/local/bin/docker";
            if (! (new File(dockerBin).exists())) {
                LOGGER.error("Docker binary not found in /usr/local/bin or /usr/bin. Creating containers will most likely fail.");
            }
        }

        return dockerClient.createContainerCmd(mesosLocalImage + ":" + registryTag)
                .withName(mesosClusterContainerName)
                .withExposedPorts(new ExposedPort(5050))
                .withEnv(createMesosLocalEnvironment());
    }

    public int getDockerPort() {
        return DOCKER_PORT;
    }

    public DockerClient getOuterDockerClient() {
        return this.dockerClient;
    }

    public String getMesosLocalImage() {
        return mesosLocalImage;
    }

    public String getRegistryTag() {
        return registryTag;
    }

}
