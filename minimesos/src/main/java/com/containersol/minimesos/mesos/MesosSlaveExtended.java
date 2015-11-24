package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.TreeMap;

public class MesosSlaveExtended extends MesosSlave {

    private static Logger LOGGER = Logger.getLogger(MesosSlaveExtended.class);

    protected final String resources;

    protected final String portNumber;

    public MesosSlaveExtended(DockerClient dockerClient, String resources, String portNumber, ZooKeeper zooKeeperContainer, String mesosLocalImage, String registryTag) {
        super(dockerClient, zooKeeperContainer);
        this.resources = resources;
        this.portNumber = portNumber;
        setMesosImageName( mesosLocalImage );
        setMesosImageTag( registryTag );
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ArrayList<ExposedPort> exposedPorts= new ArrayList<>();
        exposedPorts.add(new ExposedPort(Integer.parseInt(this.portNumber)));
        try {
            ArrayList<Integer> resourcePorts = parsePortsFromResource(this.resources);
            for (Integer port : resourcePorts) {
                exposedPorts.add(new ExposedPort(port));
            }
        } catch (Exception e) {
            LOGGER.error("Port binding is incorrect: " + e.getMessage());
        }

        CreateContainerCmd cmd = this.getBaseCommand();
        return cmd.withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]));
    }

    public String getResources() {
        return resources;
    }

    public String[] createMesosLocalEnvironment() {
        TreeMap<String, String> envs = getDefaultEnvVars();
        envs.putAll(getSharedEnvVars());
        envs.put("MESOS_RESOURCES", this.resources);

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }
}
