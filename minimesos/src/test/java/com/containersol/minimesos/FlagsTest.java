package com.containersol.minimesos;

import com.containersol.minimesos.mesos.DockerClientFactory;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.MesosMaster;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.github.dockerjava.api.DockerClient;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.TreeMap;

public class FlagsTest {

    public static final String aclExampleJson = "{ \"run_tasks\": [ { \"principals\": { \"values\": [\"foo\", \"bar\"] }, \"users\": { \"values\": [\"alice\"] } } ] }";

    // TODO (jhf@trifork.com): https://issues.apache.org/jira/browse/MESOS-3792
    public static final String aclExampleUnknownSyntaxUsedInStateJson = "run_tasks {\n  principals {\n    values: \"foo\"\n    values: \"bar\"\n  }\n  users {\n    values: \"alice\"\n  }\n}\n";

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(
            new ClusterArchitecture.Builder()
                    .withZooKeeper()
                    .withMaster(zooKeeper -> new MesosMasterEnvVars(DockerClientFactory.build(), zooKeeper)).build()
    );

    @Test
    public void clusterHasZookeeperUrl() throws UnirestException {
        Assert.assertEquals("zk://" + cluster.getZkContainer().getIpAddress() + ":2181/mesos", cluster.getMasterContainer().getFlags().get("zk"));
    }

    @Test
    public void extraEnvironmentVariablesPassedToMesosMaster() throws UnirestException {
        Assert.assertEquals("true", cluster.getMasterContainer().getFlags().get("authenticate"));
        Assert.assertEquals(aclExampleUnknownSyntaxUsedInStateJson, cluster.getMasterContainer().getFlags().get("acls"));
    }

    public static class MesosMasterEnvVars extends MesosMaster {

        protected MesosMasterEnvVars(DockerClient dockerClient, ZooKeeper zooKeeperContainer) {
            super(dockerClient, zooKeeperContainer);
        }

        @Override
        protected String[] createMesosLocalEnvironment() {
            TreeMap<String, String> envs = getDefaultEnvVars();
            envs.put("MESOS_AUTHENTICATE", "true");
            envs.put("MESOS_ACLS", aclExampleJson);

            return envs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        }
    }
}


