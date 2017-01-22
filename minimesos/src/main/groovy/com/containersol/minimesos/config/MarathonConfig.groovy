package com.containersol.minimesos.config

import com.containersol.minimesos.MinimesosException;

public class MarathonConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String MARATHON_IMAGE = "mesosphere/marathon"
    public static final String MARATHON_IMAGE_TAG = "v1.3.5"
    public static final int MARATHON_PORT = 8080;
    public static final String MARATHON_CMD = "--master zk://minimesos-zookeeper:2181/mesos --zk zk://minimesos-zookeeper:2181/marathon"


    List<AppConfig> apps = new ArrayList<>();
    String cmd

    public MarathonConfig() {
        imageName = MARATHON_IMAGE
        imageTag = MARATHON_IMAGE_TAG
        cmd = MARATHON_CMD
    }

    def app(@DelegatesTo(AppConfig) Closure cl) {
        def app = new AppConfig()
        delegateTo(app, cl)

        if (app.getMarathonJson() == null) {
            throw new MinimesosException("App config must have a 'marathonJson' property")
        }

        apps.add(app)
    }

}
