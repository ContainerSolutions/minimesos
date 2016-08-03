FROM containersol/mesos-master:1.0.0-0.1.0

ADD ./build/docker/mesos-hello-world-scheduler.jar /tmp/mesos-hello-world-scheduler.jar
ADD ./build/docker/start-scheduler.sh /tmp/start-scheduler.sh

RUN chmod +x /tmp/start-scheduler.sh

ENTRYPOINT ["/tmp/start-scheduler.sh"]
