FROM ubuntu:14.04
MAINTAINER Container Solutions BV <info@container-solutions.com>

# Let's start with some basic stuff.
RUN apt-get update -qq && apt-get install -qqy \
    apt-transport-https \
    ca-certificates \
    curl \
    lxc \
    iptables

RUN echo "deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu trusty main" > /etc/apt/sources.list.d/openjdk.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv 86F44E2A && \
    apt-get update && \
    apt-get -y --no-install-recommends install curl openjdk-8-jre-headless && \
    rm -rf /var/lib/apt/lists/*

ENV DOCKER_VERSION=1.7.1
ENV DOCKER_SHA1=386f6e91114dc252a13b266fe2ac3a27e83bd0f7
RUN curl -s --continue-at - -SL "https://get.docker.com/builds/Linux/x86_64/docker-${DOCKER_VERSION}" > /usr/bin/docker && \
    chmod a+x /usr/bin/docker && \
    echo "${DOCKER_SHA1}  /usr/bin/docker" | sha1sum -c -

RUN mkdir -p /usr/local/share/minimesos

ADD minimesos.jar /usr/local/share/minimesos/minimesos.jar

ENTRYPOINT ["java",  "-Dminimesos.dir=/tmp/.minimesos", "-jar", "/usr/local/share/minimesos/minimesos.jar"]

