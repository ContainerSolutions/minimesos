FROM containersol/mesos-agent:0.28.1-2.0.20.ubuntu1404

ADD ./build/docker/mesos-hello-world-executor.jar /tmp/mesos-hello-world-executor.jar
ADD ./build/docker/start-executor.sh /tmp/start-executor.sh

RUN chmod +x /tmp/start-executor.sh

ENTRYPOINT ["/tmp/start-executor.sh"]
