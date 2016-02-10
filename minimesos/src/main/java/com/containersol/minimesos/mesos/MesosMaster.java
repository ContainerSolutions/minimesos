package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.Map;
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

    public MesosMaster(DockerClient dockerClient, String clusterId, String uuid, String containerId) {
        super(dockerClient, clusterId, uuid, containerId);
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
    protected String getRole() {
        return "master";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {

        return dockerClient.createContainerCmd(getMesosImageName() + ":" + getMesosImageTag())
                .withName( getName() )
                .withExposedPorts(new ExposedPort(MESOS_MASTER_PORT))
                .withEnv(createMesosLocalEnvironment());
    }

    public String getStateUrl() {
        return "http://" + getIpAddress() + ":" + MesosMaster.MESOS_MASTER_PORT + "/state.json";
    }

    public JSONObject getStateInfoJSON() throws UnirestException {
        return Unirest.get(getStateUrl()).asJson().getBody().getObject();
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
