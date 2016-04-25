package com.containersolutions.mesoshelloworld.executor;

import org.apache.mesos.Executor;
import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.MesosExecutorDriver;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Protos.SlaveInfo;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from https://github.com/apache/mesos/blob/0.22.1/src/examples/java/TestExecutor.java
 */
public class FrameworkExecutor implements Executor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkExecutor.class);

    public static final String RESPONSE_STRING = "Hello world";

    private Thread thread;

    public static void main(String[] args) throws Exception {
        MesosExecutorDriver driver = new MesosExecutorDriver(new FrameworkExecutor());
        if (driver.run() != Status.DRIVER_STOPPED) {
            throw new RuntimeException("Mesos Executor Driver is not stopped");
        }
    }

    @Override
    public void registered(ExecutorDriver driver,
                           ExecutorInfo executorInfo,
                           FrameworkInfo frameworkInfo,
                           SlaveInfo slaveInfo) {
        LOGGER.info("Registered executor on " + slaveInfo.getHostname());
    }

    @Override
    public void reregistered(ExecutorDriver driver, SlaveInfo executorInfo) {
        // not implemented in test framework
    }

    @Override
    public void disconnected(ExecutorDriver driver) {
        // not implemented in test framework
    }

    @Override
    public void launchTask(final ExecutorDriver driver, final TaskInfo task) {
        thread = new FrameworkTask(driver, task);
        thread.start();
    }

    @Override
    public void killTask(ExecutorDriver driver, TaskID taskId) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    @Override
    public void frameworkMessage(ExecutorDriver driver, byte[] data) {
        // not implemented in test framework
    }


    @Override
    public void shutdown(ExecutorDriver driver) {
        // not implemented in test framework
    }

    @Override
    public void error(ExecutorDriver driver, String message) {
        // not implemented in test framework
    }

}
