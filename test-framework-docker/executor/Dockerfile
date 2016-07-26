FROM containersol/mesos-agent:1.0.0-0.1.0

ADD ./build/docker/mesos-hello-world-executor.jar /tmp/mesos-hello-world-executor.jar
ADD ./build/docker/start-executor.sh /tmp/start-executor.sh

RUN chmod +x /tmp/start-executor.sh

ENTRYPOINT ["/tmp/start-executor.sh"]
