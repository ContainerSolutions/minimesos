package org.apache.mesos.mini.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class MesosContainer extends AbstractContainer {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosContainer.class);

    private static final String MESOS_LOCAL_IMAGE = "containersol/mesos-local";
    public static final String REGISTRY_TAG = "0.2.1";
    private final MesosClusterConfig clusterConfig;
    private final String registryContainerId;

    public MesosContainer(DockerClient dockerClient, MesosClusterConfig clusterConfig, String registryContainerId) {
        super(dockerClient);
        this.clusterConfig = clusterConfig;
        this.registryContainerId = registryContainerId;
    }

    String[] createMesosLocalEnvironment() {
        ArrayList<String> envs = new ArrayList<String>();
        envs.add("NUMBER_OF_SLAVES=" + clusterConfig.numberOfSlaves);
        envs.add("MESOS_QUORUM=1");
        envs.add("MESOS_ZK=zk://localhost:2181/mesos");
        envs.add("MESOS_EXECUTOR_REGISTRATION_TIMEOUT=5mins");
        envs.add("MESOS_CONTAINERIZERS=docker,mesos");
        envs.add("MESOS_ISOLATOR=cgroups/cpu,cgroups/mem");
        envs.add("MESOS_LOG_DIR=/var/log");
        for (int i = 1; i <= clusterConfig.numberOfSlaves; i++) {
            envs.add("SLAVE" + i + "_RESOURCES=" + clusterConfig.slaveResources[i - 1]);
        }
        return envs.toArray(new String[]{});
    }

    String generateMesosMasterContainerName() {
        return "mini_mesos_cluster_" + new SecureRandom().nextInt();
    }

    public String getMesosMasterURL() {
        return getIpAddress() + ":" + clusterConfig.mesosMasterPort;
    }

    @Override
    protected void pullImage() {
        pullImage(MESOS_LOCAL_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        String mesosClusterContainerName = generateMesosMasterContainerName();

        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE)
                .withName(mesosClusterContainerName)
                .withPrivileged(true)
                        // the registry container will be known as 'private-registry' to mesos-local
                .withLinks(Link.parse(registryContainerId + ":private-registry"))
                .withExposedPorts(new ExposedPort(getDockerPort()))
                .withEnv(createMesosLocalEnvironment())
                .withVolumes(new Volume("/sys/fs/cgroup"))
                .withBinds(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup:rw"));
    }

    public int getDockerPort() {
        return DOCKER_PORT;
    }

    @Override
    public void remove() {
        DockerClientConfig.DockerClientConfigBuilder innerDockerConfigBuilder = DockerClientConfig.createDefaultConfigBuilder();
        innerDockerConfigBuilder.withUri("http://" + getIpAddress() + ":2376");
        DockerClient innerDockerClient = DockerClientBuilder.getInstance(innerDockerConfigBuilder.build()).build();

        List<Container> innerContainers = innerDockerClient.listContainersCmd().exec();
        for (Container innerContainer : innerContainers) {
            LOGGER.info("Removing Mesos-Local inner container including volumes: " + innerContainer.getNames()[0]);
            innerDockerClient.removeContainerCmd(innerContainer.getId());
        }

        LOGGER.info("Removing Mesos-Local container");
        super.remove();
    }
}