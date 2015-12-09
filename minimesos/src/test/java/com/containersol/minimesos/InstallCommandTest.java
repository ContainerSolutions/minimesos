package com.containersol.minimesos;

import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.marathon.MarathonClient;
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
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    public void testMesosInstall() {

        File taskInfo = new File( "src/test/resources/test-framework-docker.json" );
        if( !taskInfo.exists() ) {
            fail( "Failed to find task info file " + taskInfo.getAbsolutePath() );
        }

        // wait for Marathon to start
        MarathonClient marathon = new MarathonClient( CLUSTER.getMarathonContainer().getIpAddress() );
        Awaitility.await("Marathon did not start responding").atMost(60, TimeUnit.SECONDS).until(marathon::isReady);

        CLUSTER.install( taskInfo );
    }

}
