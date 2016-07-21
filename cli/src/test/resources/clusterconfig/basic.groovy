package clusterconfig

minimesos {

    mapPortsToHost = true
    timeout = 60
    mesosVersion = "0.25.0"
    clusterName = "minimesos-test"

    master {
    }

    agent {

        resources {
            cpu {
                role = "logstash"
                value = 0.2
            }
            mem {
                role = "logstash"
                value = 512
            }
            disk {
                role = "*"
                value = 5120
            }
        }

        imageName = "containersol/mesos-agent"
        imageTag = "0.25.0-0.1.0"

    }

    zookeeper {
    }

    marathon {
    }

}
