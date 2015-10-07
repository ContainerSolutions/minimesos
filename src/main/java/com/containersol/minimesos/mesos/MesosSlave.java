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

public class MesosSlave extends AbstractContainer {

    private static Logger LOGGER = Logger.getLogger(MesosSlave.class);

    public final String mesosLocalImage;
    public final String registryTag;

    protected final String resources;

    protected final String portNumber;

    protected final String zkUrl;

    protected final String master;

    private final String clusterId;

    public MesosSlave(DockerClient dockerClient, String resources, String portNumber, String zkUrl, String master, String mesosLocalImage, String registryTag, String clusterId) {
        super(dockerClient);
        this.clusterId = clusterId;
        this.zkUrl = zkUrl;
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

    String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = new TreeMap<>();

        envs.put("MESOS_PORT", this.portNumber);
        envs.put("MESOS_MASTER", this.zkUrl);
        envs.put("MESOS_GLOG_v", "1");
        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        envs.put("MESOS_RESOURCES", this.resources);

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    public CreateContainerCmd getBaseCommand() {
        String dockerBin = "/usr/bin/docker";

        if (!(new File(dockerBin).exists())) {
            dockerBin = "/usr/local/bin/docker";
            if (!(new File(dockerBin).exists())) {
                LOGGER.error("Docker binary not found in /usr/local/bin or /usr/bin. Creating containers will most likely fail.");
            }
        }
        return dockerClient.createContainerCmd(mesosLocalImage + ":" + registryTag)
                .withName("minimesos-agent-" + clusterId + "-" + getRandomId())
                .withPrivileged(true)
                .withEnv(createMesosLocalEnvironment())
                .withPid("host")
                .withLinks(new Link(this.master, "mini-mesos-master"))
                .withBinds(
                        Bind.parse("/var/lib/docker:/var/lib/docker"),
                        Bind.parse("/sys/:/sys/"),
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
        String dockerBin = "/usr/bin/docker";

        if (!(new File(dockerBin).exists())) {
            dockerBin = "/usr/local/bin/docker";
            if (!(new File(dockerBin).exists())) {
                LOGGER.error("Docker binary not found in /usr/local/bin or /usr/bin. Creating containers will most likely fail.");
            }
        }
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

}
