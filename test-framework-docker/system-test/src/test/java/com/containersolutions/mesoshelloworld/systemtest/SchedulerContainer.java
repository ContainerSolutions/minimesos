package com.containersolutions.mesoshelloworld.systemtest;

import com.containersolutions.mesoshelloworld.scheduler.Configuration;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.containersol.minimesos.container.AbstractContainer;

import java.security.SecureRandom;
import java.util.stream.IntStream;

/**
 * Container for the Scheduler
 */
public class SchedulerContainer extends AbstractContainer {

    public static final String SCHEDULER_IMAGE = "containersol/mesos-hello-world-scheduler";

    public static final String SCHEDULER_NAME = "hello-world-scheduler";

    private final String mesosIp;

    protected SchedulerContainer(DockerClient dockerClient, String mesosIp) {
        super(dockerClient);
        this.mesosIp = mesosIp;
    }

    @Override
    protected void pullImage() {
        dockerClient.pullImageCmd(SCHEDULER_IMAGE);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return dockerClient
                .createContainerCmd(SCHEDULER_IMAGE)
                .withName(SCHEDULER_NAME + "_" + new SecureRandom().nextInt())
                .withEnv("JAVA_OPTS=-Xms128m -Xmx256m")
                .withExtraHosts(IntStream.rangeClosed(1, 3).mapToObj(value -> "slave" + value + ":" + mesosIp).toArray(String[]::new))
                .withCmd(Configuration.MESOS_MASTER, getMesosUrl());
    }

    public String getMesosUrl() {
        return mesosIp + ":5050";
    }
}
