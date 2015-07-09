# mini-mesos
Testing infrastructure for Mesos frameworks. 

## Overview

The basic idea is to provide simple to use test utilities to setup a Mesos cluster that can be tested against.

The Mesos cluster setup is a docker container running zookeeper with one master and a configurable number of slaves. 
The master and slaves are launched directly inside the container.

The test utilities provide mechanisms to make arbitrary docker images available inside that Mesos cluster container.
This way executors themselves could be launched as docker containers (inside that Mesos cluster container, Docker-in-docker). 

A possible testing scenario could be:
 
 1. In the test setup build an executor image and launch the Mesos cluster container with that executor image being available inside.
 2. Call the scheduler directly from your test and point to zookeeper to detect the master or passing the master URL directly.
 3. The scheduler launches a task (which runs the executor as a docker container) on a suitable slave.
 4. Poll the state of the Mesos cluster to verify that you framework is running
 5. The test utilities take care of stopping and removing the used Mesos cluster container...


## Usage

That project provides a JUnit Rule which can be included like:



```java
public class MesosClusterTest {
    @ClassRule
    public static MesosCluster cluster = new MesosCluster(MesosClusterConfig.builder()
            .numberOfSlaves(3)
            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]","ports(*):[9201-9201,9301-9301]","ports(*):[9202-9202,9302-9302]"})
            .imagesToBuild(new ImageToBuild(new File("executor"), "mesos/elasticsearch-executor")) // in our project the executor directory contains the Dockerfile to build the executor 
            .dockerInDockerImages(new String[]{"mesos/elasticsearch-executor"})
            .privateRegistryPort(15000) // Currently you have to choose an unused port by yourself (e.g. unique per Jenkins-Job)
            .build());
            
            
    @Test
    public void mesosClusterCanBeStarted() throws Exception {
    
        JSONObject stateInfo = cluster.getStateInfo();
    
        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
        
        Assert.assertTrue(cluster.getMesosMasterURL().contains(":5050"));
     }
}
```

In this snippet we're configuring the Mesos cluster to start 3 slaves with different resources. We want to make the 
docker image "mesos/elasticsearch-executor" which is build automically because we configured it with "imagesToBuild" to
be available inside the Mesos cluster container. 

Other test cases could call the scheduler directly...


## Known Issues 

When running the tests on a Mac/Windows with boot2docker you have to make sure that your hosts can route packages into that boot2docker vm.

Therefor adding a route might be one way to solve that:
```bash
 sudo route -n add 172.17.0.0/16 `boot2docker ip`

```
In this case the network  172.17.0.0/16 is the default internal boot2docke network... You might need to adapt that to your environment...  