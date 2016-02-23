FROM alpine:latest
MAINTAINER Container Solutions BV <info@container-solutions.com>

RUN apk --update add openjdk8-jre
RUN mkdir -p /usr/local/share/minimesos
ADD rest-api.jar /usr/local/share/minimesos/rest-api.jar

VOLUME ["/usr/bin/docker"]

ENTRYPOINT ["java", "-jar", "/usr/local/share/minimesos/rest-api.jar"]

