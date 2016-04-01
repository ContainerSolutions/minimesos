package com.containersol.minimesos.mesos;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.cluster.ZooKeeper;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.util.ResourceUtil;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mesos Master adds the "agent" component for Apache Mesos
 */
public class MesosAgent extends MesosContainerImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(MesosAgent.class);

    private final MesosAgentConfig config;

    private final String MESOS_AGENT_SANDBOX_DIR = "/tmp/mesos";

    public MesosAgent(ZooKeeper zooKeeperContainer) {
        this(zooKeeperContainer, new MesosAgentConfig());
    }

    public MesosAgent(ZooKeeper zooKeeperContainer, MesosAgentConfig config) {
        super(zooKeeperContainer, config);
        this.config = config;
    }

    public MesosAgent(MesosCluster cluster, String uuid, String containerId) {
        this(cluster, uuid, containerId, new MesosAgentConfig());
    }

    private MesosAgent(MesosCluster cluster, String uuid, String containerId, MesosAgentConfig config) {
        super(cluster, uuid, containerId, config);
        this.config = config;
    }

    public String getResources() {
        return config.getResources().asMesosString();
    }

    public MesosAgentConfig getConfig() {
        return config;
    }

    @Override
    public int getPortNumber() {
        return config.getPortNumber();
    }

    public CreateContainerCmd getBaseCommand() {
        String hostDir = MesosCluster.getHostDir().getAbsolutePath();
        List<Bind> binds = new ArrayList<>();
        binds.add(Bind.parse("/var/run/docker.sock:/var/run/docker.sock"));
        binds.add(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup"));
        binds.add(Bind.parse(hostDir + ":" + hostDir));
        if (getCluster().getMapAgentSandboxVolume()) {
            binds.add(Bind.parse(String.format("%s:%s:rw", hostDir + "/.minimesos/sandbox-" + getClusterId() + "/agent-" + getUuid(), MESOS_AGENT_SANDBOX_DIR)));
        }
        return DockerClientFactory.build().createContainerCmd(getMesosImageName() + ":" + getMesosImageTag())
                .withName(getName())
                .withPrivileged(true)
                .withEnv(createMesosLocalEnvironment())
                .withPid("host")
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
        exposedPorts.add(new ExposedPort(getPortNumber()));
        try {
            ArrayList<Integer> resourcePorts = ResourceUtil.parsePorts(getResources());
            for (Integer port : resourcePorts) {
                exposedPorts.add(new ExposedPort(port));
            }
        } catch (MinimesosException e) {
            LOGGER.error("Port binding is incorrect: " + e.getMessage());
        }

        return getBaseCommand()
                .withExposedPorts(exposedPorts.toArray(new ExposedPort[exposedPorts.size()]));

    }

    @Override
    public Map<String, String> getDefaultEnvVars() {
        Map<String, String> envs = new TreeMap<>();
        envs.put("MESOS_RESOURCES", getResources());
        envs.put("MESOS_PORT", String.valueOf(getPortNumber()));
        envs.put("MESOS_MASTER", getFormattedZKAddress());
        envs.put("MESOS_SWITCH_USER", "false");
        envs.put("MESOS_LOGGING_LEVEL", getLoggingLevel());
        envs.put("MESOS_WORK_DIR", MESOS_AGENT_SANDBOX_DIR);
        envs.put("SERVICE_IGNORE", "1");
        return envs;
    }
}
