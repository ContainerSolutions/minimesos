package com.containersol.minimesos.config

import com.containersol.minimesos.MinimesosException;

public class MarathonConfig extends ContainerConfigBlock implements ContainerConfig {

    public static final String MARATHON_IMAGE = "mesosphere/marathon"
    public static final String MARATHON_IMAGE_TAG = "v0.15.3"
    public static final int MARATHON_PORT = 8080;

    List<AppConfig> apps = new ArrayList<>();

    String networkMode   = DEFAULT_NETWORK_MODE

    public MarathonConfig() {
        imageName     = MARATHON_IMAGE
        imageTag      = MARATHON_IMAGE_TAG
    }

    def app(@DelegatesTo(AppConfig) Closure cl) {
        def app = new AppConfig()
        delegateTo(app, cl)

        if (app.getMarathonJson() == null) {
            throw new MinimesosException("App config must have a 'marathonJson' property")
        }

        apps.add(app)
    }

    @Override
    String getNetworkMode() {
        return networkMode
    }

    @Override
    void setNetworkMode(String networkMode) {
        if (!StringUtils.equalsIgnoreCase(networkMode, "bridge") && !StringUtils.equalsIgnoreCase(networkMode, "host")) {
            throw new RuntimeException("Property 'networkMode' can only have the values 'bridge' or 'host'")
        }
        this.networkMode = networkMode
    }

}
