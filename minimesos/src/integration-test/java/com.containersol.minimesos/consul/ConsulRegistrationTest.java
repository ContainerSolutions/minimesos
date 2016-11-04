package com.containersol.minimesos.consul;

import com.containersol.minimesos.HelloWorldContainer;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.jayway.awaitility.Awaitility;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConsulRegistrationTest {

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-consulRegistrationTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @After
    public void after() {
        DockerContainersUtil.getContainers(false).filterByName(HelloWorldContainer.CONTAINER_NAME_PATTERN).kill().remove();
    }

    @Test
    public void testRegisterServiceWithConsul() {
        CLUSTER.addAndStartProcess(new HelloWorldContainer());

        String ipAddress = DockerContainersUtil.getIpAddress(CLUSTER.getConsul().getContainerId());
        String url = String.format("http://%s:%d/v1/catalog/service/%s",
                ipAddress, ConsulConfig.CONSUL_HTTP_PORT, HelloWorldContainer.SERVICE_NAME);

        final JSONArray[] body = new JSONArray[1];

        Awaitility.await("Test container did appear in Registrator").atMost(30, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).until(() -> {
            try {
                body[0] = Unirest.get(url).asJson().getBody().getArray();
            } catch (UnirestException e) {
                throw new AssertionError(e);
            }
            Assert.assertEquals(1, body[0].length());
        });

        JSONObject service = body[0].getJSONObject(0);
        Assert.assertEquals(HelloWorldContainer.SERVICE_PORT, service.getInt("ServicePort"));
    }

    @Test
    public void testConsulShouldBeIgnored() throws UnirestException {
        String ipAddress = DockerContainersUtil.getIpAddress(CLUSTER.getConsul().getContainerId());
        String url = String.format("http://%s:%d/v1/catalog/services", ipAddress, ConsulConfig.CONSUL_HTTP_PORT);

        JSONArray body = Unirest.get(url).asJson().getBody().getArray();
        Assert.assertEquals(1, body.length());

        JSONObject service = body.getJSONObject(0);
        Assert.assertFalse(service.has("consul-server-8300"));
        Assert.assertFalse(service.has("consul-server-8301"));
        Assert.assertFalse(service.has("consul-server-8302"));
        Assert.assertFalse(service.has("consul-server-8400"));
        Assert.assertFalse(service.has("consul-server-8500"));
        Assert.assertFalse(service.has("consul-server-8600"));
    }

}
