FROM debian:jessie-backports
MAINTAINER Container Solutions BV <info@container-solutions.com>

RUN apt-get update && apt-get install -y curl openjdk-8-jdk

RUN curl https://get.docker.com/builds/Linux/x86_64/docker-1.12.0.tgz -o docker-1.12.0.tgz && \
    tar xzf docker-1.12.0.tgz && \
    mv docker/docker /usr/bin/docker && \
    chmod +x /usr/bin/docker

ADD minimesos-cli.jar /usr/local/share/minimesos/minimesos-cli.jar
