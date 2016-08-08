FROM alpine:3.3
MAINTAINER Container Solutions BV <info@container-solutions.com>

RUN apk --update add openjdk8-jre
RUN mkdir -p /usr/local/share/minimesos

VOLUME ["/usr/bin/docker"]

ENTRYPOINT ["java",  "-Dminimesos.dir=/tmp/.minimesos", "-jar", "/usr/local/share/minimesos/minimesos.jar"]

ADD minimesos-cli.jar /usr/local/share/minimesos/minimesos-cli.jar
