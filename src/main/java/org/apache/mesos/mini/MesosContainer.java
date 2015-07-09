package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Volume;
import org.apache.mesos.mini.util.DockerUtil;

import java.security.SecureRandom;
import java.util.ArrayList;

public class MesosContainer {
    private final DockerUtil dockerUtil;
    private final DockerClient dockerClient;
    private final MesosClusterConfig clusterConfig;
    String mesosMasterIP;

    public MesosContainer(DockerClient dockerClient, MesosClusterConfig clusterConfig) {
        this.dockerClient = dockerClient;
        this.clusterConfig = clusterConfig;
        dockerUtil = new DockerUtil(dockerClient);
    }

    String startMesosLocalContainer(String registryContainerName) {
        final String MESOS_LOCAL_IMAGE = "mesos-local";

        String mesosClusterContainerName = generateMesosMasterContainerName();

        dockerUtil.buildImageFromFolder(MESOS_LOCAL_IMAGE, MESOS_LOCAL_IMAGE);

        CreateContainerCmd command = dockerClient.createContainerCmd(MESOS_LOCAL_IMAGE)
                .withName(mesosClusterContainerName)
                .withPrivileged(true)
                        // the registry container will be known as 'private-registry' to mesos-local
                .withLinks(Link.parse(registryContainerName + ":private-registry"))
                .withEnv(createMesosLocalEnvironment())
                .withVolumes(new Volume("/sys/fs/cgroup"))
                .withBinds(Bind.parse("/sys/fs/cgroup:/sys/fs/cgroup:rw"));

        return dockerUtil.createAndStart(command);
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
        return mesosMasterIP + ":" + clusterConfig.mesosMasterPort;
    }
}