FROM containersol/mesos-base:0.1.0
MAINTAINER Container Solutions BV <info@container-solutions.com>

ARG MESOS_VERSION
ENV VERSION ${MESOS_VERSION}

RUN echo "deb http://repos.mesosphere.com/ubuntu trusty main" > /etc/apt/sources.list.d/mesosphere.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF && \
    apt-get update && \
    apt-get -y install mesos=${VERSION} && \
    rm -rf /var/lib/apt/lists/*

CMD ["--registry=in_memory"]
ENTRYPOINT ["mesos-master", "--no-hostname_lookup", "true"]
