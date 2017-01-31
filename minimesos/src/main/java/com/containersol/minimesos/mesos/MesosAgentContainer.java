package com.containersol.minimesos.mesos;

import com.containersol.minimesos.cluster.MesosAgent;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.containersol.minimesos.util.ResourceUtil;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Volume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.containersol.minimesos.util.EnvironmentBuilder.newEnvironment;

/**
 * Mesos Master adds the "agent" component for Apache Mesos
 */
public class MesosAgentContainer extends MesosContainerImpl implements MesosAgent {

    private MesosAgentConfig config;

    private final static String MESOS_AGENT_WORK_DIR = "/var/lib/mesos/";

    private String hostName;

    public MesosAgentContainer(MesosAgentConfig agentConfig) {
        super(agentConfig);
        this.config = agentConfig;
        this.hostName = getRole() + "-" + getUuid();
    }

    public MesosAgentContainer(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new MesosAgentConfig(cluster.getConfiguredMesosVersion()));
    }

    private MesosAgentContainer(MesosCluster cluster, String uuid, String containerId, MesosAgentConfig config) {
        super(cluster, uuid, containerId, config);
        this.config     = config;
    }

    @Override
    public String getResources() {
        return config.getResources().asMesosString();
    }

    public MesosAgentConfig getConfig() {
        return config;
    }

    @Override
    public int getServicePort() {
        return config.getPortNumber();
    }

    public CreateContainerCmd getBaseCommand() {
        String hostDir = MesosCluster.getClusterHostDir().getAbsolutePath();
        List<Bind> binds = new ArrayList<>();
        binds.add(Bind.parse("/var/run/docker.sock:/var/run/docker.sock:rw"));
        binds.add(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup"));
        binds.add(Bind.parse(hostDir + ":" + hostDir));
        if (getCluster().getMapAgentSandboxVolume()) {
            binds.add(Bind.parse(String.format("%s:%s:rw", hostDir + "/.minimesos/sandbox-" + getClusterId() + "/" + hostName, MESOS_AGENT_WORK_DIR + hostName + "/slaves")));
        }
        return DockerClientFactory.build().createContainerCmd(getImageName() + ":" + getImageTag())
            .withName(getName())
            .withHostName(hostName)
            .withPrivileged(true)
            .withVolumes(new Volume(MESOS_AGENT_WORK_DIR + hostName))
            .withEnv(newEnvironment()
                .withValues(getMesosAgentEnvVars())
                .withValues(getSharedEnvVars())
                .createEnvironment())
            .withPidMode("host")
            .withLinks(new Link(getZooKeeper().getContainerId(), "minimesos-zookeeper"))
            .withBinds(binds.stream().toArray(Bind[]::new));
    }

    @Override
    public String getRole() {
        return "agent";
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        ArrayList<ExposedPort> exposedPorts = new ArrayList<>();
        exposedPorts.add(new ExposedPort(getServicePort()));

        ArrayList<Integer> resourcePorts = ResourceUtil.parsePorts(getResources());
        for (Integer port : resourcePorts) {
            exposedPorts.add(new ExposedPort(port));
        }

        return getBaseCommand()
                .withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]));
    }

    private Map<String, String> getMesosAgentEnvVars() {
        Map<String, String> envs = new TreeMap<>();
        envs.put("GLOG_v", "1");
        envs.put("MESOS_RESOURCES", getResources());
        envs.put("MESOS_WORK_DIR", MESOS_AGENT_WORK_DIR + hostName);
        envs.put("MESOS_DOCKER_STORE_DIR", MESOS_AGENT_WORK_DIR + hostName + "/store/docker");
        envs.put("MESOS_ISOLATION", "filesystem/linux,docker/runtime,cgroups/cpu,cgroups/mem");
        envs.put("MESOS_IMAGE_PROVIDERS", "docker");
        envs.put("MESOS_SYSTEMD_ENABLE_SUPPORT", "false");
        envs.put("MESOS_PORT", String.valueOf(getServicePort()));
        envs.put("MESOS_MASTER", getFormattedZKAddress());
        envs.put("MESOS_SWITCH_USER", "false");
        envs.put("MESOS_LOGGING_LEVEL", getLoggingLevel());
        envs.put("SERVICE_IGNORE", "1");
        return envs;
    }

}
