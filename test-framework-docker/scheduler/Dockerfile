FROM containersol/mesos-master:0.28.1-2.0.20.ubuntu1404

ADD ./build/docker/mesos-hello-world-scheduler.jar /tmp/mesos-hello-world-scheduler.jar
ADD ./build/docker/start-scheduler.sh /tmp/start-scheduler.sh

RUN chmod +x /tmp/start-scheduler.sh

ENTRYPOINT ["/tmp/start-scheduler.sh"]
