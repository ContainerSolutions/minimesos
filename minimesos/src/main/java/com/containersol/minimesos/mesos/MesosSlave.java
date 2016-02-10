package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MesosCluster;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Base MesosSlave class
 */
public class MesosSlave extends MesosContainer {

    private static final Logger LOGGER = Logger.getLogger(MesosSlave.class);

    public static final String MESOS_SLAVE_IMAGE = "containersol/mesos-agent";
    public static final int DEFAULT_MESOS_SLAVE_PORT = 5051;

    public static final String DEFAULT_PORT_RESOURCES = "ports(*):[31000-32000]";
    public static final String DEFAULT_RESOURCES = DEFAULT_PORT_RESOURCES + "; cpus(*):0.2; mem(*):256; disk(*):200";

    private String mesosImageName = MESOS_SLAVE_IMAGE;

    private String resources = DEFAULT_RESOURCES;

    private int portNumber = DEFAULT_MESOS_SLAVE_PORT;

    public MesosSlave(DockerClient dockerClient, ZooKeeper zooKeeperContainer) {
        super(dockerClient, zooKeeperContainer);
    }

    public MesosSlave(DockerClient dockerClient, String clusterId, String uuid, String containerId) {
        super(dockerClient, clusterId, uuid, containerId);
    }

    public MesosSlave(DockerClient dockerClient, String resources, int portNumber, ZooKeeper zooKeeperContainer, String mesosLocalImage, String registryTag) {
        super(dockerClient, zooKeeperContainer);
        this.resources = resources;
        this.portNumber = portNumber;
        setMesosImageName( mesosLocalImage );
        setMesosImageTag(registryTag);
    }

    public String getResources() {
        return resources;
    }

    @Override
    public String getMesosImageName() {
        return mesosImageName;
    }

    public void setMesosImageName( String mesosImageName ) {
        this.mesosImageName = mesosImageName;
    }

    public CreateContainerCmd getBaseCommand() {

        String hostDir = MesosCluster.getMinimesosHostDir().getAbsolutePath();

        return dockerClient.createContainerCmd( getMesosImageName() + ":" + getMesosImageTag() )
                .withName( getName() )
                .withPrivileged(true)
                .withEnv(createMesosLocalEnvironment())
                .withPid("host")
                .withLinks(new Link(getZooKeeperContainer().getContainerId(), "minimesos-zookeeper"))
                .withBinds(
                        Bind.parse("/var/run/docker.sock:/var/run/docker.sock"),
                        Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup"),
                        Bind.parse(hostDir + ":" + hostDir)
                );
    }

    @Override
    public String getRole() {
        return "agent";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ArrayList<ExposedPort> exposedPorts= new ArrayList<>();
        exposedPorts.add(new ExposedPort(portNumber));
        try {
            ArrayList<Integer> resourcePorts = parsePortsFromResource(resources);
            for (Integer port : resourcePorts) {
                exposedPorts.add(new ExposedPort(port));
            }
        } catch (Exception e) {
            LOGGER.error("Port binding is incorrect: " + e.getMessage());
        }

        return getBaseCommand()
                .withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]));

    }

    public static ArrayList<Integer> parsePortsFromResource(String resources) throws Exception {
        String port = resources.replaceAll(".*ports\\(.+\\):\\[(.*)\\].*", "$1");
        ArrayList<String> ports = new ArrayList<>(Arrays.asList(port.split(",")));
        ArrayList<Integer> returnList = new ArrayList<>();
        for (String el : ports) {
            String firstPortFromBinding = el.trim().split("-")[0];
            if (Objects.equals(firstPortFromBinding, el.trim())) {
                throw new Exception("Port binding " + firstPortFromBinding + " is incorrect");
            }
            returnList.add(Integer.parseInt(firstPortFromBinding)); // XXXX-YYYY will return XXXX
        }
        return returnList;
    }

    @Override
    public TreeMap<String, String> getDefaultEnvVars() {
        TreeMap<String,String> envs = new TreeMap<>();
        envs.put("MESOS_RESOURCES", resources);
        envs.put("MESOS_PORT", String.valueOf(portNumber));
        envs.put("MESOS_MASTER", getFormattedZKAddress());
        envs.put("MESOS_SWITCH_USER", "false");
        return envs;
    }
}