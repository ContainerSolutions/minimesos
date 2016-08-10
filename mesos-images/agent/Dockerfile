FROM ubuntu:14.04
MAINTAINER Container Solutions BV <info@container-solutions.com>

ARG MESOS_VERSION
ENV VERSION ${MESOS_VERSION}

RUN echo "deb http://repos.mesosphere.com/ubuntu trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    echo "deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu trusty main" > /etc/apt/sources.list.d/openjdk.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv 86F44E2A && \
    apt-get update && \
    apt-get -y install mesos=${VERSION} openjdk-8-jre-headless curl unzip iptables && \
    rm -rf /var/lib/apt/lists/*

RUN curl -so /usr/bin/docker https://get.docker.com/builds/Linux/x86_64/docker-1.9.1 && chmod +x /usr/bin/docker

ENTRYPOINT ["mesos-slave", "--no-hostname_lookup"]
