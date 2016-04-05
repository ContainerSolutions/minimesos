#!/usr/bin/env bash
echo "Pre-pulling docker images to reduce build time"
docker pull containersol/mesos-agent:0.25.0-0.2.70.ubuntu1404
docker pull containersol/mesos-master:0.25.0-0.2.70.ubuntu1404
docker pull gliderlabs/registrator:v6
docker pull containersol/consul-server:0.6
docker pull mesosphere/marathon:v0.15.3
docker pull jplock/zookeeper:3.4.6
docker pull containersol/alpine3.3-java8-jre:v1
docker pull tutum/hello-world:latest
