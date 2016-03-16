FROM containersol/mesos-base:latest
MAINTAINER Container Solutions BV <info@container-solutions.com>

ENV VERSION "0.27.0-0.2.190.ubuntu1404"

RUN echo "deb http://repos.mesosphere.io/ubuntu trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF && \
    apt-get update && \
    apt-get -y install mesos=${VERSION} && \
    rm -rf /var/lib/apt/lists/*

CMD ["--registry=in_memory"]
ENTRYPOINT ["mesos-master"]
