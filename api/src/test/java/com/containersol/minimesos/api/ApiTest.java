package com.containersol.minimesos.api;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApiTest {

    @Test
    public void testInfo() throws UnirestException {
        ApiServer apiServer = new ApiServer();
        apiServer.start();

        assertEquals("No cluster is running", Unirest.get(apiServer.getServiceUrl() + "/info").asString().getBody());

        apiServer.stop();
    }

}
