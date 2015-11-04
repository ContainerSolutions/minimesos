package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MesosCluster;
import com.containersol.minimesos.container.AbstractContainer;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

import java.util.TreeMap;

/**
 * Base, unmolested Mesos master class
 */
public class MesosMaster extends AbstractContainer {
    public static final String MESOS_MASTER_IMAGE = "containersol/mesos-master";
    public static final String MESOS_IMAGE_TAG = "0.25.0-0.2.70.ubuntu1404";
    public static final int MESOS_PORT = 5050;

    protected final String zkUrl;

    public MesosMaster(DockerClient dockerClient, String zkUrl) {
        super(dockerClient);
        this.zkUrl = zkUrl;
    }

    @Override
    protected void pullImage() {
        pullImage(MESOS_MASTER_IMAGE, MESOS_IMAGE_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {

        return dockerClient.createContainerCmd(MESOS_MASTER_IMAGE + ":" + MESOS_IMAGE_TAG)
                .withName("minimesos-master-" + MesosCluster.getClusterId() + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(MESOS_PORT))
                .withEnv(createMesosLocalEnvironment());

    }

    private String[] createMesosLocalEnvironment() {
        return getMesosEnvVars().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    protected TreeMap<String, String> getMesosEnvVars() {
        TreeMap<String,String> envs = new TreeMap<>();

        envs.put("MESOS_QUORUM", "1");
        envs.put("MESOS_ZK", zkUrl);
        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        envs.put("MESOS_WORK_DIR", "/tmp/mesos");
        return envs;
    }
}
