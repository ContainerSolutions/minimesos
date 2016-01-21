FROM alpine:latest
MAINTAINER Container Solutions BV <info@container-solutions.com>

# Let's start with some basic stuff.
RUN apk --update add openjdk8-jre

RUN mkdir -p /usr/local/share/minimesos
ADD minimesos.jar /usr/local/share/minimesos/minimesos.jar

VOLUME ["/usr/bin/docker"]

ENTRYPOINT ["java",  "-Dminimesos.dir=/tmp/.minimesos", "-jar", "/usr/local/share/minimesos/minimesos.jar"]

