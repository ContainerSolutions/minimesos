package org.apache.mesos.mini.util;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aleks on 26/08/15.
 */
public interface JsonSpecParamInterface<I, T> {
    <I> I setParameter(T... p);
    void setCmd(CreateContainerCmd cmd);

}

class JsonSpecParamExpose implements JsonSpecParamInterface<JsonSpecParamExpose, Integer> {
    CreateContainerCmd containerCmd;
    ExposedPort[] ep;
    @Override
    public JsonSpecParamExpose setParameter(Integer... p) {
        List<ExposedPort> ep = new ArrayList<>();
        for (Integer o : p) { ep.add(new ExposedPort(o)); }
        this.ep = ep.toArray(new ExposedPort[ep.size()]);
        return this;
    }

    public void setCmd(CreateContainerCmd cmd) {
        this.containerCmd = cmd;
    }
}


class JsonSpecParamCmd implements JsonSpecParamInterface<JsonSpecParamExpose, String> {
    CreateContainerCmd containerCmd;
    String[] cmd;
    @Override
    public JsonSpecParamCmd setParameter(String... p) {
        List<String> cmd = new ArrayList<>();
        for (String o : p) { cmd.add(o); }
        this.cmd = cmd.toArray(new String[cmd.size()]);
        return this;
    }

    @Override
    public void setCmd(CreateContainerCmd cmd) {
        this.containerCmd = cmd;
    }
}

class JsonSpecParamVolumes implements JsonSpecParamInterface<JsonSpecParamExpose, String> {
    CreateContainerCmd containerCmd;
    Volume[] volumes;
    @Override
    public JsonSpecParamVolumes setParameter(String... p) {
        List<Volume> volumes = new ArrayList<>();
        for (String o : p) { volumes.add(new Volume(o)); }
        this.volumes = volumes.toArray(new Volume[volumes.size()]);
        return this;
    }

    @Override
    public void setCmd(CreateContainerCmd cmd) {
        this.containerCmd = cmd;
    }
}

class JsonSpecParamVolumesFrom implements JsonSpecParamInterface<JsonSpecParamExpose, String> {
    CreateContainerCmd containerCmd;
    VolumesFrom[] volumes_from;
    @Override
    public JsonSpecParamVolumesFrom setParameter(String... p) {
        List<VolumesFrom> volumes_from = new ArrayList<>();
        for (String o : p) { volumes_from.add(new VolumesFrom(o)); }
        this.volumes_from = volumes_from.toArray(new VolumesFrom[volumes_from.size()]);
        return this;
    }

    @Override
    public void setCmd(CreateContainerCmd cmd) {
        this.containerCmd = cmd;
    }
}

class JsonSpecParamLinks implements JsonSpecParamInterface<JsonSpecParamExpose, String> {
    CreateContainerCmd containerCmd;
    Link[] links;
    @Override
    public JsonSpecParamLinks setParameter(String... p) {
        List<Link> links = new ArrayList<>();
        for (String o : p) { links.add(Link.parse(o)); }
        this.links = links.toArray(new Link[links.size()]);
        return this;
    }

    @Override
    public void setCmd(CreateContainerCmd cmd) {
        this.containerCmd = cmd;
    }
}

class JsonSpecParamEnv implements JsonSpecParamInterface<JsonSpecParamExpose, String> {
    CreateContainerCmd containerCmd;
    String[] env;
    @Override
    public JsonSpecParamEnv setParameter(String... p) {
        List<String> env = new ArrayList<>();
        for (String o : p) { env.add(o); }
        this.env = env.toArray(new String[env.size()]);
        return this;
    }

    @Override
    public void setCmd(CreateContainerCmd cmd) {
        this.containerCmd = cmd;
    }
}