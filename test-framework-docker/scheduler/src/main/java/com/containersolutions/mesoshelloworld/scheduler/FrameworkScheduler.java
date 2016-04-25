package com.containersolutions.mesoshelloworld.scheduler;

import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from: https://github.com/apache/mesos/blob/0.22.1/src/examples/java/TestFramework.java
 */
public class FrameworkScheduler implements Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkScheduler.class);

    public static final double CPUS_PER_TASK = 0.1;
    public static final double MEM_PER_TASK = 128;

    public static final int MAX_OFFERS = 10;

    private final Configuration configuration;
    private int acceptedOffers = 0;

    public FrameworkScheduler(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void resourceOffers(SchedulerDriver driver, List<Offer> offers) {

        for (Offer offer : offers) {

            ResourceOffer currentOffer = new ResourceOffer(offer.getResourcesList());
            if (currentOffer.isAcceptable() && (acceptedOffers < MAX_OFFERS)) {

                LOGGER.info(
                    "Received acceptable offer " + offer.getId().getValue() + " with cpus: " + currentOffer.offerCpus +
                        " and mem: " + currentOffer.offerMem + " with ports: " + currentOffer.offerPorts);

                List<TaskInfo> newTaskList = new ArrayList<>();

                while (newTaskList.size() < configuration.getExecutorNumber() && currentOffer.isAcceptable()) {

                    TaskInfo task = new TaskInfoFactory(configuration).newTask(offer, currentOffer);
                    newTaskList.add(task);

                }

                Status status = driver.launchTasks(Collections.singletonList(offer.getId()), newTaskList);
                LOGGER.info(String.format("Launched %d tasks. Status is %s", newTaskList.size(), status.toString()));

                acceptedOffers++;

            } else if (!currentOffer.isAcceptable()) {
                LOGGER.info(
                    "Received unacceptable offer " + offer.getId().getValue() + " with cpus: " + currentOffer.offerCpus +
                        " and mem: " + currentOffer.offerMem + " with ports: " + currentOffer.offerPorts);
            }

        }

    }

    @Override
    public void offerRescinded(SchedulerDriver driver, OfferID offerId) {
        // not supported in tests. It's ok to get task rejected
    }

    @Override
    public void statusUpdate(SchedulerDriver driver, TaskStatus status) {

        LOGGER.info("Status update: task " + status.getTaskId().getValue() +
            " is in state " + status.getState().getValueDescriptor().getName());

        if (status.getState() == TaskState.TASK_LOST ||
            status.getState() == TaskState.TASK_KILLED ||
            status.getState() == TaskState.TASK_FAILED) {

            LOGGER.error("Aborting because task " + status.getTaskId().getValue() +
                " is in unexpected state " +
                status.getState().getValueDescriptor().getName() +
                " with reason '" +
                status.getReason().getValueDescriptor().getName() + "'" +
                " from source '" +
                status.getSource().getValueDescriptor().getName() + "'" +
                " with message '" + status.getMessage() + "'");
        }

    }

    @Override
    public void frameworkMessage(SchedulerDriver driver,
                                 ExecutorID executorId,
                                 SlaveID slaveId,
                                 byte[] data) {
        // not supported in the test framework
    }

    @Override
    public void slaveLost(SchedulerDriver driver, SlaveID slaveId) {
        // not supported in the test framework
    }

    @Override
    public void executorLost(SchedulerDriver driver,
                             ExecutorID executorId,
                             SlaveID slaveId,
                             int status) {
        // not supported in the test framework
    }

    @Override
    public void registered(SchedulerDriver driver,
                           FrameworkID frameworkId,
                           MasterInfo masterInfo) {
        LOGGER.info("Registered! ID = " + frameworkId.getValue());
    }

    @Override
    public void reregistered(SchedulerDriver driver, MasterInfo masterInfo) {
        // not supported in the test framework
    }

    @Override
    public void disconnected(SchedulerDriver driver) {
        // not supported in the test framework
    }

    @Override
    public void error(SchedulerDriver driver, String message) {
        LOGGER.info("Error: " + message);
    }

    class ResourceOffer {

        final List<Long> offerPorts;
        double offerCpus = 0;
        double offerMem = 0;

        public ResourceOffer(List<Resource> resourcesList) {
            offerPorts = new ArrayList<>(resourcesList.size());
            for (Resource resource : resourcesList) {
                if ("cpus".equals(resource.getName())) {
                    offerCpus += resource.getScalar().getValue();
                } else if ("mem".equals(resource.getName())) {
                    offerMem += resource.getScalar().getValue();
                } else if ("ports".equals(resource.getName())) {
                    for (Long p = resource.getRanges().getRange(0).getBegin(); p <= resource.getRanges().getRange(0).getEnd(); p++) {
                        offerPorts.add(p);
                    }
                }
            }
        }

        public boolean isAcceptable() {
            return offerCpus >= CPUS_PER_TASK && offerMem >= MEM_PER_TASK && (!offerPorts.isEmpty());
        }

    }
}
