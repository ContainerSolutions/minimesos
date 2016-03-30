package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.ClusterArchitecture;
import com.containersol.minimesos.mesos.MesosMaster;
import com.containersol.minimesos.mesos.ZooKeeper;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class FlagsTest {

    public static final String aclExampleJson = "{ \"run_tasks\": [ { \"principals\": { \"values\": [\"foo\", \"bar\"] }, \"users\": { \"values\": [\"alice\"] } } ] }";

    public static final String aclExampleUnknownSyntaxUsedInStateJson = "run_tasks {\n  principals {\n    values: \"foo\"\n    values: \"bar\"\n  }\n  users {\n    values: \"alice\"\n  }\n}\n";

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(
            new ClusterArchitecture.Builder()
                    .withZooKeeper()
                    .withMaster(zooKeeper -> new MesosMasterEnvVars(zooKeeper)).build()
    );

    @Test
    public void clusterHasZookeeperUrl() throws UnirestException {
        Assert.assertEquals("zk://" + cluster.getZkContainer().getIpAddress() + ":2181/mesos", cluster.getMasterContainer().getFlags().get("zk"));
    }

    /**
     * See https://issues.apache.org/jira/browse/MESOS-3792. Because of this bug the  acl values are represented
     * as separate key value pairs.
     */
    @Test
    public void extraEnvironmentVariablesPassedToMesosMaster() throws UnirestException {
        Assert.assertEquals("true", cluster.getMasterContainer().getFlags().get("authenticate"));
        Assert.assertEquals(aclExampleUnknownSyntaxUsedInStateJson, cluster.getMasterContainer().getFlags().get("acls"));
    }

    public static class MesosMasterEnvVars extends MesosMaster {

        protected MesosMasterEnvVars(ZooKeeper zooKeeperContainer) {
            super(zooKeeperContainer);
        }

        @Override
        protected String[] createMesosLocalEnvironment() {
            envVars.putAll(getDefaultEnvVars());
            envVars.put("MESOS_AUTHENTICATE", "true");
            envVars.put("MESOS_ACLS", aclExampleJson);

            return createEnvironment();
        }
    }
}


