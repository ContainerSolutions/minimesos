package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;

import java.util.Map;
import java.util.TreeMap;

public class MesosMasterExtended extends MesosMaster {

    private final Map<String, String> extraEnvironmentVariables;

    public MesosMasterExtended(DockerClient dockerClient, ZooKeeper zooKeeperContainer, String mesosMasterImage, String mesosImageTag, Map<String, String> extraEnvironmentVariables, Boolean exposedHostPort) {
        super(dockerClient, zooKeeperContainer);
        setMesosImageName( mesosMasterImage );
        setMesosImageTag( mesosImageTag );
        this.extraEnvironmentVariables = extraEnvironmentVariables;
        setExposedHostPort( exposedHostPort );
    }

    @Override
    protected String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = getDefaultEnvVars();

        envs.putAll(this.extraEnvironmentVariables);
        envs.putAll(getSharedEnvVars());
        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        CreateContainerCmd createContainerCmd = super.dockerCommand();
        ExposedPort exposedPort = ExposedPort.tcp(MESOS_MASTER_PORT);
        Ports portBindings = new Ports();
        if (isExposedHostPort()) {
            portBindings.bind(exposedPort, Ports.Binding(MESOS_MASTER_PORT));
        }
        createContainerCmd
                .withEnv(createMesosLocalEnvironment())
                .withPortBindings(portBindings);
        return createContainerCmd;
    }

}
