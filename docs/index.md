# minimesos

Testing infrastructure for Mesos frameworks. 

## Installing

```
$ curl https://raw.githubusercontent.com/ContainerSolutions/minimesos/master/bin/install | bash
```

This installs the minimesos jar into ``/usr/local/share/minimesos`` and the minimesos script in ``/usr/local/bin``

## System Requirements

```minimesos``` runs docker containers with 0.25.0-0.2.70.ubuntu1404 version of ```mesos```, which comes with installation of Docker 1.8.3. The
docker clients in mesos contains should be able to talk to docker server on your host machine. Therefore the host is expected to run 1.8 or higher 
version of Docker or Docker Machine. See Docker [API compatibility](https://docs.docker.com/engine/reference/api/docker_remote_api/) table. 


## Command line interface

```
$ minimesos up
Mesos: http://192.168.99.100:5050
Marathon: http://192.168.99.100:8080
$ curl -s http://172.17.2.12:5050/state.json | jq ".version"
0.25.0
$ minimesos destroy
Destroyed minimesos cluster 3878417609
```


## Java API

In this snippet we're configuring the Mesos cluster to start 3 slaves with different resources. 

```
public class MesosClusterTest {
    @ClassRule
    public static MesosCluster cluster = new MesosCluster(new ClusterArchitecture.Builder()
        .withZooKeeper()
        .withMaster()
        .withSlave("ports(*):[9200-9200,9300-9300]")
        .withSlave("ports(*):[9201-9201,9301-9301]")
        .withSlave("ports(*):[9202-9202,9302-9302]")
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

![Mini Mesos](minimesos.gif?raw=true "minimesos")

![Creative Commons Licence](cc-cc.png "Creative Commons Licence") Licenced under CC BY [remember to play](http://remembertoplay.co/) in collaboration with [Container Solutions](http://www.container-solutions.com/)

## Building and running on MAC with docker-machine

### Install DockerToolbox (including docker-machine)

Download package from <https://www.docker.com/docker-toolbox> and install it. 
Tested with [DockerToolbox-1.9.0d.pkg](https://github.com/docker/toolbox/releases/download/v1.9.0d/DockerToolbox-1.9.0d.pkg)

### Creating VM for minimesos

Create a docker machine, make sure its environment variables are visible to the test, ensure the docker containers' IP addresses are available on the host

```
$ docker-machine create -d virtualbox --virtualbox-memory 2048 --virtualbox-cpu-count 1 minimesos
$ eval $(docker-machine env minimesos)
```

When VM is ready you can either *build latest version* of minimesos or *install a released version*

### Building latest version of minimesos

In CLI

```
# changing route is required to let Java process on host to find minimesos in virtual machine.
$ sudo route delete 172.17.0.0/16; sudo route -n add 172.17.0.0/16 $(docker-machine ip ${DOCKER_MACHINE_NAME})
$ ./gradlew clean build --info --stacktrace
```

In Idea, add the ```docker-machine env minimesos``` variables to the Idea junit testing dialog. E.g.

```
DOCKER_TLS_VERIFY=1
DOCKER_HOST=tcp://192.168.99.100:2376
DOCKER_CERT_PATH=/home/user/.docker/machine/machines/minimesos
```

One of the minimesos build results is new docker image. E.g.

```
$ docker images
REPOSITORY                      TAG                     IMAGE ID            CREATED             VIRTUAL SIZE
containersol/minimesos          latest                  cf854cfb1865        2 minutes ago       529.3 MB
```

Running ```./gradlew install``` will make latest version of minimesos script available on the PATH

### Installing a released vesion of minimesos on VM

Install minimesos on MAC

```
$ curl -sSL https://raw.githubusercontent.com/ContainerSolutions/minimesos/master/bin/install | sudo sh
```

The command above makes minimesos script available on the PATH

### Running minimesos from CLI

To create minimesos cluster execute ```minimesos up```. It will create temporary container with minimesos process, which will start other containers and will exit.
When cluster is started ```.minimesos/minimesos.cluster``` file with cluster ID is created in local directory. This file is destroyed with ```minimesos destroy```

```
$ minimesos up
Mesos: http://192.168.99.100:5050
Marathon: http://192.168.99.100:8080
$ curl -s http://172.17.2.12:5050/state.json | jq ".version"
0.25.0
$ minimesos destroy
Destroyed minimesos cluster 3878417609
```

The `minimesos up` command supports `--exposedHostPorts` flag, that automatically binds Mesos and Marathon ports `5050`, resp. `8080` to the host machine, providing you with easy access to the services. Let the following table explain what the host machine is in different contexts:

| --exposedHostPorts | Linux                            | Max OS X                            |
|--------------------|----------------------------------|-------------------------------------|
| disabled           | container IP addresses (default) | n/a                                 |
| enabled            | host computer                    | docker-machine IP address (default) |

Having `--exposedHostPorts` enabled on Linux makes minimesos containers effectively accessible to anyone who has network access to your computer.
We don't recommend this. Not using `--exposedHostPorts` flag on Max OS X on the other hand makes the containers inaccessible, because they run inside another virtual machine. This machine is typically managed by `docker-machine`.
Minimesos tries to choose the appropriate configuration for your system automatically.

### Mappings of volumes

The table below is an attempt to summarize mappings, which enable execution of minimesos

| MAC Host        | boot2docker VM        | minimesos container           |
| --------------- | --------------------- | ----------------------------- |
| $PWD/.minimesos | $PWD/.minimesos       | /tmp/.minimesos               |
|                 | /var/lib/docker       | /var/lib/docker               |
|                 | /var/run/docker.sock  | /var/run/docker.sock          |
|                 | /usr/local/bin/docker | /usr/local/bin/docker         |
|                 | /sys/fs/cgroup        | /sys/fs/cgroup                |


## Caveats

`minimesos up` command supports `--mesosImageTag` parameter, which can be used to override the version of Mesos to be used. 
When running an older version of Mesos, you may encounter [compatibility issues between Mesos 0.22 and Docker v. greater than 1.7](https://issues.apache.org/jira/browse/INFRA-10621).

Since version 0.3.0 minimesos uses 'flat' container structure, which means that all containers (agents, master, zookeeper) as well as all Docker executor tasks are run in the same Docker context - host machine.
This has following benefits:
  1. Shared repository with the host Docker
  2. Transparency of your test-cluster.
  3. Ability to keep track of executor tasks
  4. Easy access to the logs

However, you should account for this when developing a Mesos framework.
By default, Mesos starts Docker containerized executor tasks with the ```--host``` mode.
Libprocess tries to bind on a loopback interface and fails to establish communication with the master node.

To work around this, start the executor using [```--bridge``` mode](https://issues.apache.org/jira/browse/MESOS-1621) and provide LIBPROCESS_IP environment variable with the IP address of the executor container, for example using this:

``` 
export LIBPROCESS_IP=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -n 1)

```

This ensures your executor task will be assigned an interface to allow communication within the cluster.


