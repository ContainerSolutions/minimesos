package com.containersol.minimesos;

import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.junit.MesosClusterTestRule;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthenticationTest {

    public static final String aclExampleUnknownSyntaxUsedInStateJson = "run_tasks {\n  principals {\n    values: \"foo\"\n    values: \"bar\"\n  }\n  users {\n    values: \"alice\"\n  }\n}\n";

    @ClassRule
    public static final MesosClusterTestRule RULE = MesosClusterTestRule.fromFile("src/test/resources/configFiles/minimesosFile-authenticationTest");

    public static MesosCluster CLUSTER = RULE.getMesosCluster();

    @Test
    public void clusterHasZookeeperUrl() throws UnirestException {
        assertEquals("zk://" + CLUSTER.getZooKeeper().getIpAddress() + ":2181/mesos", CLUSTER.getMaster().getState().getFlags().get("zk"));
    }

    /**
     * See https://issues.apache.org/jira/browse/MESOS-3792. Because of this bug the  acl values are represented
     * as separate key value pairs.
     */
    @Test
    public void extraEnvironmentVariablesPassedToMesosMaster() throws UnirestException {
        assertEquals("true", CLUSTER.getMaster().getState().getFlags().get("authenticate"));
        assertEquals(aclExampleUnknownSyntaxUsedInStateJson, CLUSTER.getMaster().getState().getFlags().get("acls"));
    }

}
