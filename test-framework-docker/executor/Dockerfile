FROM containersol/mesos-agent:0.25.0-0.2.70.ubuntu1404

ADD ./build/docker/mesos-hello-world-executor.jar /tmp/mesos-hello-world-executor.jar
ADD ./build/docker/start-executor.sh /tmp/start-executor.sh

RUN chmod +x /tmp/start-executor.sh

ENTRYPOINT ["/tmp/start-executor.sh"]
