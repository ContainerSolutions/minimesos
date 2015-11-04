package com.containersol.minimesos.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import org.apache.log4j.Logger;
import com.containersol.minimesos.container.AbstractContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.TreeMap;

@Deprecated
public class MesosSlaveExtended extends MesosSlave {

    private static Logger LOGGER = Logger.getLogger(MesosSlaveExtended.class);

    public final String mesosLocalImage;
    public final String registryTag;

    protected final String resources;

    protected final String portNumber;

    protected final String master;

    private final String clusterId;

    public MesosSlaveExtended(DockerClient dockerClient, String resources, String portNumber, String zkUrl, String master, String mesosLocalImage, String registryTag, String clusterId) {
        super(dockerClient, zkUrl);
        this.clusterId = clusterId;
        this.resources = resources;
        this.portNumber = portNumber;
        this.master = master;
        this.mesosLocalImage = mesosLocalImage;
        this.registryTag = registryTag;
    }

    @Override
    public void start() {
        super.start();
    }

    public CreateContainerCmd getBaseCommand() {

        String dockerBin = "/usr/bin/docker";
        File dockerBinFile = new File(dockerBin);
        if (!(dockerBinFile.exists() && dockerBinFile.canExecute())) {
            dockerBin = "/usr/local/bin/docker";
            dockerBinFile = new File(dockerBin);
            if (!(dockerBinFile.exists() && dockerBinFile.canExecute() )) {
                LOGGER.error("Docker binary not found in /usr/local/bin or /usr/bin. Creating containers will most likely fail.");
            }
        }

        return dockerClient.createContainerCmd(mesosLocalImage + ":" + registryTag)
                .withName("minimesos-agent-" + clusterId + "-" + getRandomId())
                .withPrivileged(true)
                .withEnv(createMesosLocalEnvironment())
                .withPid("host")
                .withLinks(new Link(this.master, "minimesos-master"))
                .withBinds(
                        Bind.parse("/var/lib/docker:/var/lib/docker"),
                        Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup"),
                        Bind.parse(String.format("%s:/usr/bin/docker", dockerBin)),
                        Bind.parse("/var/run/docker.sock:/var/run/docker.sock")
                );
    }

    @Override
    protected void pullImage() {
        pullImage(mesosLocalImage, registryTag);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ArrayList<ExposedPort> exposedPorts= new ArrayList<>();
        exposedPorts.add(new ExposedPort(Integer.parseInt(this.portNumber)));
        try {
            ArrayList<Integer> resourcePorts = this.parsePortsFromResource(this.resources);
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

    public ArrayList<Integer> parsePortsFromResource(String resources) throws Exception {
        return super.parsePortsFromResource(resources);
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String, String> envs = getDefaultEnvVars();
        envs.put("MESOS_RESOURCES", this.resources);

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }
}
