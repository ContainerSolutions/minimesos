#!/bin/bash
java $JAVA_OPTS -Djava.library.path=/usr/local/lib -jar /tmp/mesos-hello-world-executor.jar $@