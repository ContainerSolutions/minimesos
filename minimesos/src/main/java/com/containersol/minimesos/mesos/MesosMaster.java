package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Base, unmolested Mesos master class
 */
public class MesosMaster extends MesosContainer {

    // is here for future extension of Master configuration
    private final MesosMasterConfig config;

    public MesosMaster(DockerClient dockerClient, ZooKeeper zooKeeperContainer) {
        this(dockerClient, zooKeeperContainer, new MesosMasterConfig());
    }

    public MesosMaster(DockerClient dockerClient, ZooKeeper zooKeeperContainer, MesosMasterConfig config) {
        super(dockerClient, zooKeeperContainer, config);
        this.config = config;
    }

    public MesosMaster(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId) {
        this(dockerClient, cluster, uuid, containerId, new MesosMasterConfig());
    }

    private MesosMaster(DockerClient dockerClient, MesosCluster cluster, String uuid, String containerId, MesosMasterConfig config) {
        super(dockerClient, cluster, uuid, containerId, config);
        this.config = config;
    }

    @Override
    public int getPortNumber() {
        return MesosMasterConfig.MESOS_MASTER_PORT;
    }

    @Override
    public TreeMap<String, String> getDefaultEnvVars() {
        TreeMap<String, String> envs = new TreeMap<>();
        envs.put("MESOS_QUORUM", "1");
        envs.put("MESOS_ZK", getFormattedZKAddress());
        envs.put("MESOS_LOGGING_LEVEL", getLoggingLevel());
        if (getCluster() != null) {
            envs.put("MESOS_CLUSTER", getCluster().getClusterName());
        }
        return envs;
    }

    @Override
    public String getRole() {
        return "master";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {

        int port = getPortNumber();
        ExposedPort exposedPort = ExposedPort.tcp(port);

        Ports portBindings = new Ports();
        if (getCluster().isExposedHostPorts()) {
            portBindings.bind(exposedPort, Ports.Binding(port));
        }

        return dockerClient.createContainerCmd(getMesosImageName() + ":" + getMesosImageTag())
                .withName( getName() )
                .withExposedPorts(new ExposedPort(getPortNumber()))
                .withEnv(createMesosLocalEnvironment())
                .withPortBindings(portBindings);
    }

    public Map<String, String> getFlags() throws UnirestException {
        JSONObject flagsJson = this.getStateInfoJSON().getJSONObject("flags");
        Map<String, String> flags = new TreeMap<>();
        for (Object key : flagsJson.keySet()) {
            String keyString = (String) key;
            String value = flagsJson.getString(keyString);
            flags.put(keyString, value);
        }
        return flags;
    }

}
