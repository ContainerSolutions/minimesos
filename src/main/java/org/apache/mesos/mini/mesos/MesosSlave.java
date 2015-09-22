package org.apache.mesos.mini.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.io.File;
import java.security.SecureRandom;
import java.util.TreeMap;

public class MesosSlave extends AbstractContainer {

    private static final int DOCKER_PORT = 2376;

    private static Logger LOGGER = Logger.getLogger(MesosSlave.class);

    private static final String MESOS_LOCAL_IMAGE = "redjack/mesos-slave";
    public static final String REGISTRY_TAG = "0.21.0";

    protected final String resources;

    protected final String zkIp;

    private final MesosClusterConfig clusterConfig;

    public MesosSlave(DockerClient dockerClient, MesosClusterConfig clusterConfig, String zkIp, String resources) {
        super(dockerClient);
        this.clusterConfig = clusterConfig;
        this.zkIp = zkIp;
        this.resources = resources;
    }

    @Override
    public void start() {
        super.start();
    }

    String[] createMesosLocalEnvironment() {
        TreeMap<String,String> envs = new TreeMap<>();

        envs.put("NUMBER_OF_SLAVES", Integer.toString(clusterConfig.numberOfSlaves));
        envs.put("MESOS_QUORUM", "1");
//        envs.put("MESOS_ZK", "zk://" + zkIp  + ":2181/mesos");
        envs.put("MESOS_MASTER", "zk://" + zkIp + ":2181/mesos");
        envs.put("MESOS_EXECUTOR_REGISTRATION_TIMEOUT", "5mins");
        envs.put("MESOS_CONTAINERIZERS", "docker,mesos");
        envs.put("MESOS_ISOLATOR", "cgroups/cpu,cgroups/mem");
        envs.put("MESOS_LOG_DIR", "/var/log");
        envs.put("MESOS_RESOURCES", this.resources);
//        for (int i = 1; i <= clusterConfig.numberOfSlaves; i++) {
//            envs.put("SLAVE" + i + "_RESOURCES", clusterConfig.slaveResources[i - 1]);
//        }
//        envs.putAll(clusterConfig.extraEnvironmentVariables);

        return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
    }

    String generateMesosMasterContainerName() {
        return "mesos-cluster-slave_" + new SecureRandom().nextInt();
    }


    @Override
    protected void pullImage() {
        pullImage(MESOS_LOCAL_IMAGE, REGISTRY_TAG);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        String mesosClusterContainerName = generateMesosMasterContainerName();
        String dockerBin = "/usr/bin/docker";

        if (! (new File(dockerBin).exists())) {
            dockerBin = "/usr/local/bin/docker";
            if (! (new File(dockerBin).exists())) {
                LOGGER.error("Docker binary not found in /usr/local/bin or /usr/bin. Creating containers will most likely fail.");
            }
        }

        return dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE + ":" + REGISTRY_TAG)
                .withName(mesosClusterContainerName)
                .withPrivileged(true)

                .withExposedPorts(new ExposedPort(5051))
//                .withNetworkMode("host")
                .withEnv(createMesosLocalEnvironment())
                .withBinds(
                        Bind.parse("/sys:/sys"),
                        Bind.parse(String.format("%s:/usr/bin/docker", dockerBin)),
                        Bind.parse("/var/run/docker.sock:/var/run/docker.sock")
                )
                ;
    }

    public int getDockerPort() {
        return DOCKER_PORT;
    }

    public DockerClient getOuterDockerClient() {
        return this.dockerClient;
    }

}
