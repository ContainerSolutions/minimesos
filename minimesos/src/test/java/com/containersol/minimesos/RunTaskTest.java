package com.containersol.minimesos;

import com.containersol.minimesos.container.AbstractContainer;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.jayway.awaitility.Awaitility;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created by alg on 1/27/16.
 */
public class RunTaskTest {
    private static DockerClient dockerClient = DockerClientFactory.build();

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(
            new ClusterArchitecture.Builder()
                    .withZooKeeper()
                    .withMaster()
                    .withSlave()
                    .withSlave()
                    . build());

    @After
    public void after() {
        DockerContainersUtil util = new DockerContainersUtil(dockerClient);
        util.getContainers(false).filterByName("^mesos-[0-9a-f\\-]*S\\d*\\.[0-9a-f\\-]*$").remove();
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

        AbstractContainer mesosSlave = new AbstractContainer(
                dockerClient) {

            @Override
            protected String getRole() {
                return "slave";
            }

            @Override
            protected void pullImage() {}

            @Override
            protected CreateContainerCmd dockerCommand() {
                return dockerClient.createContainerCmd( "containersol/mesos-agent:0.25.0-0.2.70.ubuntu1404" )
                        .withName("minimesos-execute-" + getClusterId() + "-" + getUuid())
                        .withEntrypoint(
                                "mesos-execute",
                                "--master=" + cluster.getMesosMasterContainer().getIpAddress() + ":5050",
                                "--command=echo 1",
                                "--name=test-cmd",
                                "--resources=cpus:0.1;mem:128"
                        );
            }
        };

        cluster.addAndStartContainer(mesosSlave, MesosContainer.DEFAULT_TIMEOUT_SEC);
        LogContainerTestCallback cb = new LogContainerTestCallback();
        dockerClient.logContainerCmd(mesosSlave.getContainerId()).withStdOut().exec(cb);
        cb.awaitCompletion();

        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            LogContainerTestCallback cb1 = new LogContainerTestCallback();
            dockerClient.logContainerCmd(mesosSlave.getContainerId()).withStdOut().exec(cb1);
            cb1.awaitCompletion();
            String log = cb1.toString();
            if (log.contains("Received status update TASK_FINISHED for task test-cmd")) {
                return true;
            }
            return false;
        });
    }

}
