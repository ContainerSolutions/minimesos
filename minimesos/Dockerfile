FROM ubuntu:14.04
MAINTAINER Container Solutions BV <info@container-solutions.com>

ENV VERSION "feature/docker-machine"

# Let's start with some basic stuff.
RUN apt-get update -qq && apt-get install -qqy \
    apt-transport-https \
    ca-certificates \
    curl \
    lxc \
    iptables

RUN echo "deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu trusty main" > /etc/apt/sources.list.d/openjdk.list && \
    echo "deb http://get.docker.com/ubuntu docker main" > /etc/apt/sources.list.d/docker.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv 86F44E2A && \
    apt-key adv --keyserver pgp.mit.edu --recv-keys 36A1D7869245C8950F966E92D8576A8BA88D21E9 && \
    apt-get update && \
    apt-get -y --no-install-recommends install curl openjdk-8-jre-headless && \
    apt-get install -y lxc-docker-1.7.1 && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/local/share/minimesos

ADD minimesos.jar /usr/local/share/minimesos/minimesos.jar

ENTRYPOINT ["java",  "-Dminimesos.dir=/tmp/.minimesos", "-jar", "/usr/local/share/minimesos/minimesos.jar"]

