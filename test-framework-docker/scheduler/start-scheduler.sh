#!/bin/bash
java $JAVA_OPTS -Djava.library.path=/usr/lib -jar /tmp/mesos-hello-world-scheduler.jar $@