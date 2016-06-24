FROM containersol/alpine3.3-java8-jre:v1
MAINTAINER Container Solutions BV <info@container-solutions.com>

ADD minimesos-api.jar /usr/local/share/minimesos/minimesos-api.jar

ENTRYPOINT java -Dminimesos.dir=/tmp/.minimesos -jar /usr/local/share/minimesos/minimesos-api.jar
