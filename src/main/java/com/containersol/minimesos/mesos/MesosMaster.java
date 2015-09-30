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

    private static final String MESOS_LOCAL_IMAGE = "mesosphere/mesos-master";
    public static final String REGISTRY_TAG = "0.23.0-1.0.ubuntu1404";

    private final String zkIp;
    private final String zkPath;

    public MesosMaster(DockerClient dockerClient, String zkIp, String zkPath) {
        super(dockerClient);
        this.zkIp = zkIp;
        this.zkPath = zkPath;
    }

    @Override
    public void start() {
        super.start();
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = new TreeMap<>();

        envs.put("MESOS_QUORUM", "1");
        envs.put("MESOS_ZK", "zk://" + zkIp + ":2181/" + this.zkPath);
        envs.put("MESOS_MASTER", "zk://" + zkIp + ":2181/mesos" + this.zkPath);
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
        pullImage(MESOS_LOCAL_IMAGE, REGISTRY_TAG);
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

        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
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

}
