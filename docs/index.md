# minimesos introduction

The experimentation and testing tool for Apache Mesos. `minimesos` is a tool created for a quick and easy creation of a Mesos cluster. This is achieved by running Mesos processes in Docker containers. `minimesos` implements simple to remember and discover CLI commands that allow creating and destroying local Mesos cluster in seconds.

If you have used Vagrant and Docker before, the set of the commands will be very familiar to you, if you have not - don't worry! We will walk you through them.

## Resources

 - Website https://minimesos.org/
 - Blog https://minimesos.org/blog
 - Interactive tutorial https://minimesos.org/try


## System Requirements

minimesos runs Docker containers with a configurable version version of Mesos. See the [minimesos-docker](https://github.com/ContainerSolutions/minimesos-docker) repository
with an overview of the images supported by minimesos.

The Docker client in these Mesos images should be able to talk to Docker daemon on your host machine. The Docker daemon is expected to run version 1.11.0 or higher
of Docker or Docker Machine. See Docker [API compatibility](https://docs.docker.com/engine/reference/api/docker_remote_api/) table.



## Installing

```
$ curl -sSL https://minimesos.org/install | sh
```

This installs the minimesos binary into ``${HOME}/.minimesos/bin``

You can add it to your executables search path using following command:
```
$ export PATH=$PATH:$HOME/.minimesos/bin
```

Once the installation has been successful, let's try running ```minimesos --help```
This should print the list of all possible commands and command line arguments.


These are the options you might want to change to configure your cluster.

## Command line interface

```
Usage: minimesos [options] [command] [command options]
  Options:
    --debug
       Enable debug logging.
       Default: false
    --help, -help, -?, -h
       Show help
       Default: false
  Commands:
    help      Display help
      Usage: help [options]

    init      Initialize a minimesosFile
      Usage: init [options]

    install      Install a framework with Marathon
      Usage: install [options]
        Options:
          --marathonFile
             Marathon JSON app install file location. Either this or --stdin
             parameter must be used
          --stdin
             Use JSON from standard import. Allow piping JSON from other
             processes. Either this or --marathonFile parameter must be used
             Default: false

    destroy      Destroy a minimesos cluster
      Usage: destroy [options]

    up      Create a minimesos cluster
      Usage: up [options]
        Options:
          --clusterConfig
             Path to file with cluster configuration. Defaults to minimesosFile
             Default: minimesosFile
          --mapPortsToHost
             Map the Mesos and Marathon UI ports on the host level (we
             recommend to enable this on Mac (e.g. when using docker-machine) and disable
             on Linux).
             Default: false
          --num-agents
             Number of agents to start
             Default: -1
          --timeout
             Time to wait for a container to get responsive, in seconds.
             Default: 60

    state      Display state.json file of a master or an agent
      Usage: state [options]
        Options:
          --agent
             Specify an agent to query, otherwise query a master
             Default: <empty string>

    info      Display cluster information
      Usage: info [options]
```

## minimesosFile and ```minimesos init```
minimesos config is stored in `minimesosFile`, the file that is generated with sensible defaults when running ```minimesos init```

Again, you might notice similarity with ```vagrant init``` and Vagrantfile.
Open the minimesosFile and let's look at the list of the blocks.

The configuration file is a list of blocks, logically grouped by curly brackets ```{ }```
Scalar values are simple key-value strings.

| Option name           | type    | Meaning                                                                            |
|-----------------------|---------|------------------------------------------------------------------------------------|
| clusterName           | String  | The name of the Mesos cluster                                                      |
| mapPortsToHost        | Boolean | Whether to map container ports to the host network                                     |
| loggingLevel          | String  | Debug level in the terminal output                                                 |
| mapAgentSandboxVolume | Boolean | Creates a volume mapping to the agent sandbox under ${PWD}/.minimesos/sandbox-.../ |
| mesosVersion          | String  | Mesos version                                                                      |
| timeout               | Integer | Amount of seconds to wait for the cluster to become alive before giving up         |
| agent                 | Block   | Describes a single instance of a mesos agent                                       |
| agent resources       | Block   | Describes resources of the mesos agent                                             |
| agent resources cpu   | Block   | Describes CPU resources                                                            |
| agent resources mem   | Block   | Describes memory resources                                                         |
| agent resources ports | Block   | Describes network ports resources                                                  |

## Consul and registrator
By default, minimesos starts consul and registrator containers giving you ability to configure service discovery.

## Java API

In this snippet we're configuring the Mesos cluster to start 3 agents with different resources.

```
public class MesosClusterTest {

    @ClassRule
    public static MesosClusterTestRule testRule =
        MesosClusterTestRule.fromFile("src/test/resources/configFiles/testMinimesosFile");

    public static MesosCluster cluster = testRule.getMesosCluster();

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

 1. In the test setup launch the Mesos cluster container
 2. Call the scheduler directly from your test and point to Zookeeper to detect the master or passing the master URL directly.
 3. The scheduler launches a task on a suitable agent.
 4. Poll the state of the Mesos cluster to verify that you framework is running
 5. The test utilities take care of stopping and removing the Mesos cluster

![minimesos](minimesos.png?raw=true "minimesos")

![Creative Commons Licence](cc-cc.png "Creative Commons Licence") Licenced under CC BY [remember to play](http://remembertoplay.co/) in collaboration with [Container Solutions](http://www.container-solutions.com/)

## Building and running on MAC with Docker Machine

### Install DockerToolbox (including Docker Machine)

Download package from <https://www.docker.com/docker-toolbox> and install it.
Tested with [DockerToolbox-1.9.0d.pkg](https://github.com/docker/toolbox/releases/download/v1.9.0d/DockerToolbox-1.9.0d.pkg)

### Creating VM for minimesos

Create a docker machine, make sure its environment variables are visible to the test, ensure the docker containers' IP addresses are available on the host

```
$ docker-machine create -d virtualbox --virtualbox-memory 8192 --virtualbox-cpu-count 1 --engine-opt dns=8.8.8.8 minimesos
$ eval $(docker-machine env minimesos)
```

When VM is ready you can either *build latest version* of minimesos or *install a released version*

### Building latest version of minimesos

In a terminal window, run the following commands:

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
containersol/minimesos-cli      latest                  cf854cfb1865        2 minutes ago       529.3 MB
```

Running ```./gradlew install``` will make latest version of minimesos script available on the PATH

### Running minimesos from CLI

To create minimesos cluster execute ```minimesos up```. It will create temporary container with minimesos process, which will start other containers and will exit.
When cluster is started ```.minimesos/minimesos.cluster``` file with cluster ID is created in local directory. This cluster is destroyed with ```minimesos destroy```

```
$ minimesos init
Initialized minimesosFile in this directory

$ minimesos up
Minimesos cluster is running: 3878417609
Mesos version: 0.25.0
export MINIMESOS_NETWORK_GATEWAY=172.17.0.1
export MINIMESOS_AGENT=http://172.17.0.5:5051; export MINIMESOS_AGENT_IP=172.17.0.5
export MINIMESOS_ZOOKEEPER=zk://172.17.0.3:2181/mesos; export MINIMESOS_ZOOKEEPER_IP=172.17.0.3
export MINIMESOS_MARATHON=http://172.17.0.6:8080; export MINIMESOS_MARATHON_IP=172.17.0.6
export MINIMESOS_CONSUL=http://172.17.0.7:8500; export MINIMESOS_CONSUL_IP=172.17.0.7
export MINIMESOS_MASTER=http://172.17.0.4:5050; export MINIMESOS_MASTER_IP=172.17.0.4

$ minimesos state | jq ".version"
0.25.0

$ minimesos destroy
Destroyed minimesos cluster 3878417609
```

The `minimesos up` command supports `--mapPortsToHost` flag, that automatically binds Mesos and Marathon ports `5050`, resp. `8080` to the host machine, providing you with easy access to the services. Let the following table explain what the host machine is in different contexts:

| --mapPortsToHost   | Linux                            | OS X                                |
|--------------------|----------------------------------|-------------------------------------|
| disabled           | container IP addresses (default) | n/a                                 |
| enabled            | host computer                    | docker-machine IP address (default) |

Having `--mapPortsToHost` enabled on Linux makes minimesos containers effectively accessible to anyone who has network access to your computer.
We don't recommend this. Not using `--mapPortsToHost` flag on Max OS X on the other hand makes the containers inaccessible, because they run inside another virtual machine. This machine is typically managed by `docker-machine`.
Minimesos tries to choose the appropriate configuration for your system automatically.

An other alternative if you use docker-machine, is to access the reported IP address in browser, it's necessary to add routing of docker IP range to IP address of the docker machine

```
sudo route delete 172.17.0.0/16; sudo route -n add 172.17.0.0/16 $(docker-machine ip ${DOCKER_MACHINE_NAME})
```

### Volume maappings

The table below show the volume mappings, on the host, on Docker machine and in the minimesos container.

| OSX Host        | Docker Machine        | minimesos container           |
| --------------- | --------------------- | ----------------------------- |
| $PWD/.minimesos | $PWD/.minimesos       | /tmp/.minimesos               |
|                 | /var/lib/docker       | /var/lib/docker               |
|                 | /var/run/docker.sock  | /var/run/docker.sock          |
|                 | /usr/local/bin/docker | /usr/local/bin/docker         |
|                 | /sys/fs/cgroup        | /sys/fs/cgroup                |


## Caveats

`minimesos up` command supports `--mesosImageTag` parameter, which can be used to override the version of Mesos to be used.
When running an older version of Mesos, you may encounter [compatibility issues between Mesos 0.22 and Docker v. greater than 1.7](https://issues.apache.org/jira/browse/INFRA-10621).

Since version 0.3.0 minimesos uses 'flat' container structure, which means that all containers (agents, master, zookeeper) as well as all Docker executor tasks are run in the same Docker context - the host machine.
This has following benefits:
  1. Shared repository with the host Docker
  2. Transparency of your test cluster.
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
