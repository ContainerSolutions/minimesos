package org.apache.mesos.mini.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.security.SecureRandom;
import java.util.List;
import java.util.TreeMap;

public class MesosContainer extends AbstractContainer {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosContainer.class);

    private static final String MESOS_LOCAL_IMAGE = "containersol/mesos-local";
    public static final String REGISTRY_TAG = "dood";

    private final MesosClusterConfig clusterConfig;
    private DockerClient outerDockerClient;

    public MesosContainer(DockerClient dockerClient, MesosClusterConfig clusterConfig) {
        super(dockerClient);
        this.clusterConfig = clusterConfig;
    }

    @Override
    public void start() {
        super.start();

        DockerClientConfig.DockerClientConfigBuilder outerDockerClient;
        outerDockerClient = DockerClientConfig.createDefaultConfigBuilder();
        outerDockerClient.withUri("http://" + getIpAddress() + ":" + getDockerPort());
        this.outerDockerClient = DockerClientBuilder.getInstance(outerDockerClient.build()).build();
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = new TreeMap<>();

        envs.put("NUMBER_OF_SLAVES", Integer.toString(clusterConfig.numberOfSlaves));
        envs.put("MESOS_QUORUM", "1");
        envs.put("MESOS_ZK", "zk://localhost:2181/mesos");

        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        for (int i = 1; i <= clusterConfig.numberOfSlaves; i++) {
            envs.put("SLAVE" + i + "_RESOURCES", clusterConfig.slaveResources[i - 1]);
        }
        envs.putAll(clusterConfig.extraEnvironmentVariables);

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
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

        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
                .withName(mesosClusterContainerName)
                .withPrivileged(true)
                        // the registry container will be known as 'private-registry' to mesos-local
                .withExposedPorts(new ExposedPort(getDockerPort()))
                .withEnv(createMesosLocalEnvironment())
                .withVolumes(new Volume("/sys/fs/cgroup"))
                .withBinds(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup:rw"),
                           Bind.parse("/usr/bin/docker:/usr/bin/docker"),
                           Bind.parse("/var/run/docker.sock:/var/run/docker.sock"));
    }

    public int getDockerPort() {
        return DOCKER_PORT;
    }

    public DockerClient getOuterDockerClient() {
        return outerDockerClient;
    }

}
