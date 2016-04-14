FROM containersol/mesos-master:0.25.0-0.2.70.ubuntu1404

ADD ./build/docker/mesos-hello-world-scheduler.jar /tmp/mesos-hello-world-scheduler.jar
ADD ./build/docker/start-scheduler.sh /tmp/start-scheduler.sh

RUN chmod +x /tmp/start-scheduler.sh

ENTRYPOINT ["/tmp/start-scheduler.sh"]
