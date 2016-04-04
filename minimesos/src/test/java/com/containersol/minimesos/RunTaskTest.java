package com.containersol.minimesos;

import com.containersol.minimesos.cluster.ClusterMember;
import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.docker.DockerClientFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.jayway.awaitility.Awaitility;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class RunTaskTest {
    private static final String TASK_CLUSTER_ROLE = "test";

    @ClassRule
    public static final MesosClusterTestRule cluster = new MesosClusterTestRule(
            new ClusterArchitecture.Builder()
                    .withZooKeeper()
                    .withMaster()
                    .withAgent()
                    .withAgent()
                    .build());

    @After
    public void after() {
        DockerContainersUtil util = new DockerContainersUtil();
        // kill() is not used because containers are expected to exit by this time
        util.getContainers(true).filterByName("^minimesos-" + TASK_CLUSTER_ROLE + "-[0-9a-f\\-]*$").remove();
    }


    public static class LogContainerTestCallback extends LogContainerResultCallback {
        protected final StringBuffer log = new StringBuffer();

        @Override
        public void onNext(Frame frame) {
            log.append(new String(frame.getPayload()));
            super.onNext(frame);
        }

        @Override
        public String toString() {
            return log.toString();
        }
    }

    @Test
    public void testMesosExecuteContainerSuccess() throws InterruptedException {

        ClusterMember mesosAgent = new AbstractContainer() {

            @Override
            public String getRole() {
                return TASK_CLUSTER_ROLE;
            }

            @Override
            protected void pullImage() {}

            @Override
            protected CreateContainerCmd dockerCommand() {
                return DockerClientFactory.build().createContainerCmd("containersol/mesos-agent:0.25.0-0.2.70.ubuntu1404")
                        .withName( getName() )
                        .withEntrypoint(
                                "mesos-execute",
                                "--master=" + cluster.getMaster().getIpAddress() + ":5050",
                                "--command=echo 1",
                                "--name=test-cmd",
                                "--resources=cpus:0.1;mem:128"
                        );
            }
        };

        cluster.addAndStartContainer(mesosAgent);
        LogContainerTestCallback cb = new LogContainerTestCallback();
        DockerClientFactory.build().logContainerCmd(mesosAgent.getContainerId()).withStdOut().exec(cb);
        cb.awaitCompletion();

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            LogContainerTestCallback cb1 = new LogContainerTestCallback();
            DockerClientFactory.build().logContainerCmd(mesosAgent.getContainerId()).withStdOut().exec(cb1);
            cb1.awaitCompletion();
            String log = cb1.toString();
            return log.contains("Received status update TASK_FINISHED for task test-cmd");
        });
    }

}
