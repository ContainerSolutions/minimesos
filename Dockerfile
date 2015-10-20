FROM ubuntu:14.04
MAINTAINER Container Solutions BV <info@container-solutions.com>

ENV VERSION "feature/docker-machine"

RUN echo "deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu trusty main" > /etc/apt/sources.list.d/openjdk.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv 86F44E2A && \
    apt-get update && \
    apt-get -y --no-install-recommends install curl openjdk-8-jre-headless && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/local/share/minimesos

ADD build/libs/minimesos.jar /usr/local/share/minimesos/minimesos.jar

ENTRYPOINT ["java",  "-jar", "/usr/local/share/minimesos/minimesos.jar"]
