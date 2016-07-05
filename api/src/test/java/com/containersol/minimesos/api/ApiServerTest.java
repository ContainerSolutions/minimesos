package com.containersol.minimesos.api;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ApiServerTest {

    private static ApiServer apiServer;

    @BeforeClass
    public static void before() {
        apiServer = new ApiServer();
        apiServer.start();
    }

    @AfterClass
    public static void after() {
        apiServer.stop();
    }

    @Test
    public void testInfo() throws UnirestException {
        HttpResponse<String> response = Unirest.get(apiServer.getServiceUrl() + "/info").asString();

        assertEquals("No cluster is running", response.getBody());
    }

    @Test
    public void testStart() throws UnirestException, IOException {
        HttpResponse<String> firstResponse = Unirest.post(apiServer.getServiceUrl() + "/start").body(IOUtils.toByteArray(new FileInputStream("src/test/resources/minimesosFile-apiServer"))).asString();

        assertEquals(200, firstResponse.getStatus());

        JSONObject jsonObject = new JSONObject(firstResponse.getBody());
        String clusterId = jsonObject.getString("clusterId");

        MesosCluster mesosCluster = MesosCluster.loadCluster(clusterId, new MesosClusterContainersFactory());
        assertEquals(1, mesosCluster.getAgents().size());

        HttpResponse<String> secondResponse = Unirest.post(apiServer.getServiceUrl() + "/start").body(IOUtils.toByteArray(new FileInputStream("src/test/resources/minimesosFile-apiServer"))).asString();

        assertEquals(200, firstResponse.getStatus());
        assertEquals(200, secondResponse.getStatus());

        assertEquals(firstResponse.getBody(), secondResponse.getBody());
    }

}
