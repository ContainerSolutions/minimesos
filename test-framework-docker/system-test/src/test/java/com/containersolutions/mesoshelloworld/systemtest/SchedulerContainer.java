package com.containersolutions.mesoshelloworld.systemtest;

import com.containersol.minimesos.container.AbstractContainerImpl;
import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersolutions.mesoshelloworld.scheduler.Configuration;
import com.github.dockerjava.api.command.CreateContainerCmd;

import java.util.stream.IntStream;

/**
 * Container for the Scheduler
 */
public class SchedulerContainer extends AbstractContainerImpl {

    public static final String SCHEDULER_IMAGE = "containersol/mesos-hello-world-scheduler";
    public static final String SCHEDULER_NAME = "hello-world-scheduler";

    private static int containerCount = 0;

    private final String mesosIp;
    private final int containerIndex;

    protected SchedulerContainer(String mesosIp) {
        super();

        this.mesosIp = mesosIp;
        containerCount++;
        containerIndex = containerCount;

    }

    @Override
    public String getRole() {
        return "helloworld-scheduler";
    }

    @Override
    protected void pullImage() {
        DockerClientFactory.build().pullImageCmd(SCHEDULER_IMAGE);
    }

    @Override
    protected CreateContainerCmd dockerCommand() {
        return DockerClientFactory.build()
                .createContainerCmd(SCHEDULER_IMAGE)
                .withName( getName() )
                .withEnv("JAVA_OPTS=-Xms128m -Xmx256m")
                .withExtraHosts(IntStream.rangeClosed(1, 3).mapToObj(value -> "slave" + value + ":" + mesosIp).toArray(String[]::new))
                .withCmd(Configuration.MESOS_MASTER, getMesosUrl());
    }

    public String getMesosUrl() {
        return mesosIp + ":5050";
    }

    @Override
    public String getName() {
        return SCHEDULER_NAME + "_" + containerIndex;
    }

}
