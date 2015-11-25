FROM alpine:3.2
MAINTAINER Container Solutions BV <info@container-solutions.com>

ENV VERSION "feature/docker-machine"

# Install curl, tar and glibc (needed by Oracle JDK)
RUN apk -q --update add curl tar && \
    curl -Ls https://circle-artifacts.com/gh/andyshinn/alpine-pkg-glibc/6/artifacts/0/home/ubuntu/alpine-pkg-glibc/packages/x86_64/glibc-2.21-r2.apk > /tmp/glibc-2.21-r2.apk && \
    apk add -q --allow-untrusted /tmp/glibc-2.21-r2.apk &&\
    rm -rf /var/cache/apk/*

ENV JDK_VERSION=8u65-b17
ENV JDK_SHA1=7cfb221ae3d3939db24c4e88992ddd1a48388379
ENV PATH $PATH:/jdk/bin
RUN curl -sjkSL -H "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/${JDK_VERSION}/jdk-8u65-linux-x64.tar.gz" > jdk.tar.gz && \
    echo "${JDK_SHA1}  jdk.tar.gz" | sha1sum -c - && \
    mkdir jdk && \
    tar xfz jdk.tar.gz -C jdk --strip-components 1 && \
    rm -rf /jdk/*src.zip \
           /jdk/lib/missioncontrol \
           /jdk/lib/visualvm \
           /jdk/lib/*javafx* \
           /jdk/jre/lib/plugin.jar \
           /jdk/jre/lib/ext/jfxrt.jar \
           /jdk/jre/bin/javaws \
           /jdk/jre/lib/javaws.jar \
           /jdk/jre/lib/desktop \
           /jdk/jre/plugin \
           /jdk/jre/lib/deploy* \
           /jdk/jre/lib/*javafx* \
           /jdk/jre/lib/*jfx* \
           jdk.tar.gz    

ENV DOCKER_VERSION=1.7.1
ENV DOCKER_SHA1=386f6e91114dc252a13b266fe2ac3a27e83bd0f7
RUN curl -s --continue-at - -SL "https://get.docker.com/builds/Linux/x86_64/docker-${DOCKER_VERSION}" > /usr/bin/docker && \
    chmod a+x /usr/bin/docker && \
    echo "${DOCKER_SHA1}  /usr/bin/docker" | sha1sum -s -c -

RUN mkdir -p /usr/local/share/minimesos

ADD minimesos.jar /usr/local/share/minimesos/minimesos.jar

ENTRYPOINT ["java",  "-Dminimesos.dir=/tmp/.minimesos", "-jar", "/usr/local/share/minimesos/minimesos.jar"]
