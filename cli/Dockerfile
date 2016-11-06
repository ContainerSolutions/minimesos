FROM containersol/alpine3.3-java8-jre:v1
MAINTAINER Container Solutions BV <info@container-solutions.com>

RUN apk add --update curl libstdc++&& \
        rm -rf /var/cache/apk/*

RUN curl https://get.docker.com/builds/Linux/x86_64/docker-1.12.0.tgz -o docker-1.12.0.tgz && \
    tar xzf docker-1.12.0.tgz && \
    mv docker/docker /usr/bin/docker && \
    chmod +x /usr/bin/docker

ADD minimesos-cli.jar /usr/local/share/minimesos/minimesos-cli.jar
