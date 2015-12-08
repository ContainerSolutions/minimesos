package com.containersol.minimesos;

import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class InstallCommandTest {

    protected static final DockerClient dockerClient = DockerClientFactory.build();
    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder(dockerClient)
            .withZooKeeper()
            .withMaster()
            .withSlave("ports(*):[8080-8082]")
            .withMarathon(zooKeeper -> new Marathon(dockerClient, zooKeeper, true ))
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @After
    public void after() {
        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);
        util.getContainers(true).filterByName("^mesos-[0-9a-f\\-]*S\\d*\\.[0-9a-f\\-]*$").remove();
    }

    @Test
    @Ignore
    public void testMesosIntstall() {
        CLUSTER.install("/Users/vik/projects/cs/devbox/sources/mini-mesos/minimesos/src/test/resources/marathon.json");
    }

}
