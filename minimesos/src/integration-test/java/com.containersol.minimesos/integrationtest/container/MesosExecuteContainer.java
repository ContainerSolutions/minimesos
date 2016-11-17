package com.containersol.minimesos.integrationtest.container;

import com.containersol.minimesos.integrationtest.MesosClusterTest;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ContainerConfigBlock;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;

public class MesosExecuteContainer extends AbstractContainer {

    private static final String TASK_CLUSTER_ROLE = "test";

    public MesosExecuteContainer() {
        super(new ContainerConfigBlock(MesosAgentConfig.MESOS_AGENT_IMAGE, ClusterConfig.DEFAULT_MESOS_CONTAINER_TAG));
    }

    @Override
    public String getRole() {
        return TASK_CLUSTER_ROLE;
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.build().createContainerCmd(String.format("%s:%s", MesosAgentConfig.MESOS_AGENT_IMAGE, ClusterConfig.DEFAULT_MESOS_CONTAINER_TAG))
            .withName(getName())
            .withEntrypoint(
                "mesos-execute",
                "--master=" + MesosClusterTest.CLUSTER.getMaster().getIpAddress() + ":5050",
                "--command=echo 1",
                "--name=test-cmd",
                "--resources=cpus:0.1;mem:128"
            );
    }
}
