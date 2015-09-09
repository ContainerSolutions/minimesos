package org.apache.mesos.mini.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;

public class MesosContainer extends AbstractContainer {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosContainer.class);

    private static final String MESOS_LOCAL_IMAGE = "containersol/mesos-local";
    public static final String REGISTRY_TAG = "14";

    private final MesosClusterConfig clusterConfig;
    private final String registryContainerId;
    private DockerClient innerDockerClient;

    public MesosContainer(DockerClient dockerClient, MesosClusterConfig clusterConfig, String registryContainerId) {
        super(dockerClient);
        this.clusterConfig = clusterConfig;
        this.registryContainerId = registryContainerId;
    }

    @Override
    public void start() {
        super.start();

        await().atMost(10, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(new DockerSocketIsAvailable<Boolean>(this));

        String os = System.getProperty("os.name");
        DockerClientConfig.DockerClientConfigBuilder innerDockerConfigBuilder;
        innerDockerConfigBuilder = DockerClientConfig.createDefaultConfigBuilder();
        innerDockerConfigBuilder.withUri("http://" + getIpAddress() + ":" + getDockerPort());
        this.innerDockerClient = DockerClientBuilder.getInstance(innerDockerConfigBuilder.build()).build();
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
        List<Container> innerContainers = innerDockerClient.listContainersCmd().exec();
        for (Container innerContainer : innerContainers) {
            LOGGER.info("Removing Mesos-Local inner container including volumes: " + innerContainer.getNames()[0]);
            innerDockerClient.removeContainerCmd(innerContainer.getId()).withForce().withRemoveVolumes(true).exec();
        }

        LOGGER.info("Removing Mesos-Local container");
        super.remove();
    }

    public DockerClient getInnerDockerClient() {
        return innerDockerClient;
    }

    class DockerSocketIsAvailable<T> implements Callable<Boolean> {

        private MesosContainer container;

        public DockerSocketIsAvailable(MesosContainer container) {
            this.container = container;
        }

        @Override
        public Boolean call() throws Exception {
            try (Socket ignored = new Socket(container.getIpAddress(), container.getDockerPort())) {
                return true;
            } catch (IOException ignored) {
                return false;
            }
        }
    }

}
