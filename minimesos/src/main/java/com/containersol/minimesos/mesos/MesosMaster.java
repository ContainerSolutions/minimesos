package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MesosCluster;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;

import java.util.TreeMap;

/**
 * Base, unmolested Mesos master class
 */
public class MesosMaster extends MesosContainer {

    public static final String MESOS_MASTER_IMAGE = "containersol/mesos-master";
    public static final int MESOS_MASTER_PORT = 5050;

    private String mesosImageName = MESOS_MASTER_IMAGE;
    private boolean exposedHostPort;

    public MesosMaster(DockerClient dockerClient, ZooKeeper zooKeeperContainer) {
        super(dockerClient, zooKeeperContainer);
    }

    @Override
    public String getMesosImageName() {
        return mesosImageName;
    }

    public void setMesosImageName( String mesosImageName ) {
        this.mesosImageName = mesosImageName;
    }

    public boolean isExposedHostPort() {
        return exposedHostPort;
    }
    public void setExposedHostPort(boolean exposedHostPort) {
        this.exposedHostPort = exposedHostPort;
    }

    @Override
    public TreeMap<String, String> getDefaultEnvVars() {
        TreeMap<String,String> envs = new TreeMap<>();
        envs.put("MESOS_QUORUM", "1");
        envs.put("MESOS_ZK", getFormattedZKAddress());
        return envs;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {

        return dockerClient.createContainerCmd(MESOS_MASTER_IMAGE + ":" + MESOS_IMAGE_TAG)
                .withName("minimesos-master-" + MesosCluster.getClusterId() + "-" + getRandomId())
                .withExposedPorts(new ExposedPort(MESOS_MASTER_PORT))
                .withEnv(createMesosLocalEnvironment());
    }
}
