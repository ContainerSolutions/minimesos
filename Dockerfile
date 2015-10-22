FROM containersol/jre8-docker:v0.0.1

MAINTAINER Container Solutions BV <info@container-solutions.com>

RUN mkdir -p /usr/local/share/minimesos

ADD minimesos.jar /usr/local/share/minimesos/minimesos.jar

ENTRYPOINT ["java",  "-Duser.home=/tmp/minimesos", "-jar", "/usr/local/share/minimesos/minimesos.jar"]
