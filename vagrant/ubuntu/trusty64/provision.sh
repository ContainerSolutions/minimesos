#!/usr/bin/env bash

###
###  JDK and Gradle
###

###
###  Getting Docker installed
###
echo ""
echo "Getting Docker"

apt-get update
apt-get -qqy install apt-transport-https ca-certificates
apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D

echo "deb https://apt.dockerproject.org/repo ubuntu-trusty main" > /etc/apt/sources.list.d/docker.list
apt-get update

apt-get -qqy install linux-image-extra-$(uname -r)
apt-get -qqy install docker-engine=1.9.1-0~trusty

gpasswd -a vagrant docker
service docker start

apt-get -qqy install language-pack-UTF-8

echo ""
echo "To build minimesos do:"
echo "> vagrant ssh"
echo "> cd /minimesos/source"
echo "> ./pull-containers.sh"
echo "> ./gradlew clean build --info --stacktrace"
echo ""


