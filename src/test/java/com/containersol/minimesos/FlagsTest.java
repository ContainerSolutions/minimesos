package com.containersol.minimesos;

import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.containersol.minimesos.mesos.MesosClusterConfig;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;

public class FlagsTest {

    public static final String aclExampleJson = "{ \"run_tasks\": [ { \"principals\": { \"values\": [\"foo\", \"bar\"] }, \"users\": { \"values\": [\"alice\"] } } ] }";

    // TODO (jhf@trifork.com): https://issues.apache.org/jira/browse/MESOS-3792
    public static final String aclExampleUnknownSyntaxUsedInStateJson = "run_tasks {\n  principals {\n    values: \"foo\"\n    values: \"bar\"\n  }\n  users {\n    values: \"alice\"\n  }\n}\n";

    @ClassRule
    public static final MesosCluster cluster = new MesosCluster(
            MesosClusterConfig.builder()
                    .zkUrl("mesos")
                    .slaveResources(new String[]{
                            "ports(*):[9201-9201, 9301-9301]; cpus(*):0.2; mem(*):256; disk(*):200",
                            "ports(*):[9202-9202, 9302-9302]; cpus(*):0.2; mem(*):256; disk(*):200",
                            "ports(*):[9203-9203, 9303-9303]; cpus(*):0.2; mem(*):256; disk(*):200"
                    })
                    .extraEnvironmentVariables(new HashMap<String, String>() {{
                        this.put("MESOS_AUTHENTICATE", "true");
                        this.put("MESOS_ACLS", aclExampleJson);
                    }})
                    .build()
    );

    @Test
    public void clusterHasZookeeperUrl() throws UnirestException {
        Assert.assertEquals("zk://" + cluster.getZkContainer().getIpAddress() + ":2181/mesos", cluster.getFlags().get("zk"));
    }

    @Test
    public void extraEnvironmentVariablesPassedToMesosMaster() throws UnirestException {
        Assert.assertEquals("true", cluster.getFlags().get("authenticate"));
        Assert.assertEquals(aclExampleUnknownSyntaxUsedInStateJson, cluster.getFlags().get("acls"));
    }
}


