package com.containersol.minimesos.config

import com.containersol.minimesos.MinimesosException;

public class MarathonConfig extends GroovyBlock implements ContainerConfig {

    public static final String MARATHON_IMAGE = "mesosphere/marathon"
    public static final String MARATHON_IMAGE_TAG = "v0.15.3"
    public static final int MARATHON_PORT = 8080;

    String imageName     = MARATHON_IMAGE
    String imageTag      = MARATHON_IMAGE_TAG

    List<AppConfig> apps = new ArrayList<>();

    def app(@DelegatesTo(AppConfig) Closure cl) {
        def app = new AppConfig()
        delegateTo(app, cl)

        if (app.getFile() == null && app.getUrl() == null) {
            throw new MinimesosException("App config must have either a 'marathonJsonUrl' or a 'marathonJsonPath' property")
        }

        apps.add(app)
    }

}
