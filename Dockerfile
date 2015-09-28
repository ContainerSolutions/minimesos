FROM containersol/dind

MAINTAINER Container Solutions BV

ENV DEBIAN_FRONTEND noninteractive
ENV VERSION 0.22.1
ENV PKG_RELEASE 1.0

RUN apt-get update -qq && apt-get upgrade -qq

# Make sure to install OpenJDK 6 explicitly.  The libmesos library includes an
# RPATH entry, which is needed to find libjvm.so at runtime.  This RPATH is
# hard-coded to the OpenJDK version that was present when the package was
# compiled.  So even though the Debian package claims that it works with either
# OpenJDK 6 or OpenJDK 7, the fact that Mesosphere compiled with OpenJDK 6 means
# that we have to have that specific version present at runtime.

WORKDIR /tmp
RUN apt-get install -qqy openjdk-6-jre-headless curl python2.7 && \
  curl -s -O https://downloads.mesosphere.io/master/ubuntu/14.04/mesos_${VERSION}-${PKG_RELEASE}.ubuntu1404_amd64.deb && \
  dpkg --unpack mesos_${VERSION}-${PKG_RELEASE}.ubuntu1404_amd64.deb && \
  apt-get install -f -y && \
  rm mesos_${VERSION}-${PKG_RELEASE}.ubuntu1404_amd64.deb && \
  apt-get clean

RUN curl -s -O http://apache.mirrors.pair.com/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz
RUN tar -xzf zookeeper-3.4.6.tar.gz -C /opt
RUN mv /opt/zookeeper-3.4.6 /opt/zookeeper
RUN cp /opt/zookeeper/conf/zoo_sample.cfg /opt/zookeeper/conf/zoo.cfg
RUN mkdir -p /tmp/zookeeper

# add script to run cluster
RUN mkdir /opt/mesos_test_cluster
ADD ./run.sh /opt/mesos_test_cluster/
RUN chmod +x /opt/mesos_test_cluster/run.sh

# Install Supervisord
RUN apt-get install -qqy supervisor

# Make the necessary folders for Supervisord
RUN mkdir -p /var/log/supervisor /etc/supervisor/conf.d

# Add the base configuration file for Supervisord
ADD supervisor.conf /etc/supervisor.conf

# add slave configuration
CMD /opt/mesos_test_cluster/run.sh
