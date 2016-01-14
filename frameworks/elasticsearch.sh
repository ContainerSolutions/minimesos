#!/bin/sh 
echo<<OEF
{
"id": "elasticsearch-mesos-scheduler",
"container": {
"docker": {
"image": "mesos/elasticsearch-scheduler",
"network": "BRIDGE"
}
},
"args": ["--zookeeperMesosUrl", "${MINIMESOS_ZOOKEEPER}", "--useIpAddress", "true"],
"cpus": 0.2,
"mem": 512.0,
"env": {
"JAVA_OPTS": "-Xms128m -Xmx256m"
},
"instances": 1
}
EOF;

