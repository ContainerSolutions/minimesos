#!/bin/bash


buildVersion() {
    cd $1

    cd agent
    docker build -t containersol/mesos-agent:$1 .
    cd ..

    cd master
    docker build -t containersol/mesos-master:$1 .
    cd ..
    cd ..
}

cd base
docker build -t containersol/mesos-base:latest .
cd ..

buildVersion "0.25.0-0.2.70.ubuntu1404"
buildVersion "0.26.0-0.2.145.ubuntu1404"
buildVersion "0.27.0-0.2.190.ubuntu1404"
buildVersion "0.27.1-2.0.226.ubuntu1404"
