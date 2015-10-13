# Mini Mesos

Testing infrastructure for Mesos frameworks. 

## Installing

```
$ sudo curl http://minimesos.org/install | bash
```

This installs the minimesos jar into ``/usr/local/share/minimesos`` and the minimesos script in ``/usr/local/bin``

## Command line interface

```
$ minimesos up
http://172.17.2.12:5050
$ curl -s http://172.17.2.12:5050/state.json | jq ".version"
0.22.1
$ minimesos destroy
Destroyed minimesos cluster 3878417609
```

## Java API

In this snippet we're configuring the Mesos cluster to start 3 slaves with different resources. 

```
public class MesosClusterTest {
    @ClassRule
    public static MesosCluster cluster = new MesosCluster(MesosClusterConfig.builder()
            .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]","ports(*):[9201-9201,9301-9301]","ports(*):[9202-9202,9302-9302]"})
            .build());
            
    @Test
    public void mesosClusterCanBeStarted() throws Exception {
        JSONObject stateInfo = cluster.getStateInfoJSON();
        Assert.assertEquals(3, stateInfo.getInt("activated_slaves"));
        Assert.assertTrue(cluster.getMesosMasterURL().contains(":5050"));
     }
}
```
## TDD for Mesos frameworks

A possible testing scenario could be:
 
 1. In the test setup  launch the Mesos cluster container
 2. Call the scheduler directly from your test and point to zookeeper to detect the master or passing the master URL directly.
 3. The scheduler launches a task on a suitable slave.
 4. Poll the state of the Mesos cluster to verify that you framework is running
 5. The test utilities take care of stopping and removing the Mesos cluster

![Mini Mesos](mini-mesos.gif?raw=true "Mini Mesos")

![Creative Commons Licence](cc-cc.png "Creative Commons Licence") Licenced under CC BY [remember to play](http://remembertoplay.co/) in collaboration with [Container Solutions](http://www.container-solutions.com/)

## Running on a mac

Create a docker machine, make sure its environment variables are
visible to the test, ensure the docker containers' IP addresses are
available on the host, and then build and run the tests:

```
# latest version of boot2docker.iso cannot be used
$ docker-machine create -d virtualbox --virtualbox-memory 2048 --virtualbox-cpu-count 1 --virtualbox-boot2docker-url https://github.com/boot2docker/boot2docker/releases/download/v1.7.1/boot2docker.iso mini-mesos
$ eval $(docker-machine env mini-mesos)
$ sudo route delete 172.17.0.0/16; sudo route -n add 172.17.0.0/16 $(docker-machine ip ${DOCKER_MACHINE_NAME})
$ ./gradlew clean build --info --stacktrace
```

In Idea, add the ```docker-machine env mini-mesos``` variables to the idea junit testing dialog. E.g.

```
DOCKER_TLS_VERIFY=1
DOCKER_HOST=tcp://192.168.99.100:2376
DOCKER_CERT_PATH=/home/user/.docker/machine/machines/mini-mesos
```

### Installing docker-machine on mac

Due to dependencies among versions of Docker, Mesos and docker-machine latest versions of can not be used
 
```
$ brew install homebrew/versions/docker171
$ brew link docker171
$ brew install docker-machine
```


