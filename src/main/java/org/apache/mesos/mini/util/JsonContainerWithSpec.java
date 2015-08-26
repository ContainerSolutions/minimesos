package org.apache.mesos.mini.util;

/**
 * Created by aleks on 26/08/15.
 */
class JsonContainerWithSpec {
    public void setExpose(JsonSpecParamExpose expose) {
        this.expose = expose;
    }

    public void setCmd(JsonSpecParamCmd cmd) {
        this.cmd = cmd;
    }

    public void setVolumes(JsonSpecParamVolumes volumes) {
        this.volumes = volumes;
    }

    public void setVolumes_from(JsonSpecParamVolumesFrom volumes_from) {
        this.volumes_from = volumes_from;
    }

    public void setLinks(JsonSpecParamLinks links) {
        this.links = links;
    }

    public void setEnvironment(JsonSpecParamEnv environment) {
        this.environment = environment;
    }

    public String name;
    public JsonSpecParamExpose expose;
    public JsonSpecParamCmd cmd;
    public JsonSpecParamVolumes volumes;
    public JsonSpecParamVolumesFrom volumes_from;
    public JsonSpecParamEnv environment;
    public JsonSpecParamLinks links;
}
