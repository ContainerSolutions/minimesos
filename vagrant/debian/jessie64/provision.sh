#!/usr/bin/env bash

###
###  JDK and Gradle
###

echo "deb http://ftp.debian.org/debian jessie-backports main" > /etc/apt/sources.list.d/openjdk.list && \
apt-get update -qq && apt-get install -qqy \
    apt-transport-https \
    curl \
    unzip \
    openjdk-8-jdk

curl https://downloads.gradle.org/distributions/gradle-2.12-bin.zip --output /tmp/gradle-2.12-bin.zip --silent
unzip -q /tmp/gradle-2.12-bin.zip -d /usr/share

echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre" >> /home/vagrant/.profile
echo "export GRADLE_HOME=/usr/share/gradle-2.12" >> /home/vagrant/.profile
echo "export PATH=\$JAVA_HOME/bin:\$GRADLE_HOME/bin:\$PATH" >> /home/vagrant/.profile

update-ca-certificates -f

###
###  Getting Docker installed
###
echo ""
echo "Getting Docker"

echo "deb https://apt.dockerproject.org/repo debian-jessie main" > /etc/apt/sources.list.d/docker.list && \
	apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D && \
	apt-get update -qq && \
	apt-get -qqy install docker-engine=1.9.1-0~jessie

echo "Enabling non-sudo access to docker"
gpasswd -a vagrant docker

service docker start

echo ""
echo "To build minimesos do:"
echo "> vagrant ssh"
echo "> cd /minimesos/source"
echo "> ./pull-containers.sh"
echo "> ./gradlew clean build --info --stacktrace"
echo ""


