package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.marathon.Marathon;
import com.containersol.minimesos.mesos.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONObject;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.PrintStream;
import java.util.*;

import static org.junit.Assert.*;

public class ExposedPortsTest {

    private static final boolean EXPOSED_PORTS = true;

    protected static final String resources = MesosSlave.DEFAULT_PORT_RESOURCES + "; cpus(*):0.2; mem(*):256; disk(*):200";
    protected static final DockerClient dockerClient = DockerClientFactory.build();

    protected static final ClusterArchitecture CONFIG = new ClusterArchitecture.Builder(dockerClient)
            .withZooKeeper()
            .withMaster(zooKeeper -> new MesosMasterExtended(dockerClient, zooKeeper, MesosMaster.MESOS_MASTER_IMAGE, MesosContainer.MESOS_IMAGE_TAG, new TreeMap<>(), EXPOSED_PORTS ))
            .withSlave(zooKeeper -> new MesosSlave(dockerClient, resources, 5051, zooKeeper, MesosSlave.MESOS_SLAVE_IMAGE, MesosContainer.MESOS_IMAGE_TAG))
            .withMarathon(zooKeeper -> new Marathon(dockerClient, zooKeeper, true))
            .build();

    @ClassRule
    public static final MesosCluster CLUSTER = new MesosCluster(CONFIG);

    @After
    public void after() {
        DockerContainersUtil util = new DockerContainersUtil(CONFIG.dockerClient);
        util.getContainers(false).filterByName( HelloWorldContainer.CONTAINER_NAME_PATTERN ).kill().remove();
    }

    @Test
    public void testLoadCluster() {

        String clusterId = CLUSTER.getClusterId();
        MesosCluster cluster = MesosCluster.loadCluster(clusterId);

        assertTrue( "Deserialize cluster is expected to remember exposed ports setting", cluster.isExposedHostPorts() );
    }

}
