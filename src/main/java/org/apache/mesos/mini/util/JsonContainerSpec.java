package org.apache.mesos.mini.util;

import com.github.dockerjava.api.DockerClient;
import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.apache.mesos.mini.container.AbstractContainer;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by aleks on 17/08/15.
 */
public class JsonContainerSpec {

    Logger logger = Logger.getLogger(JsonContainerSpec.class);

    private final String jsonSpec;
    private final DockerClient dockerClient;

    public JsonContainerSpec(String jsonSpec, DockerClient dockerClient) {
        this.jsonSpec = jsonSpec;
        this.dockerClient = dockerClient;
    }

    // Json spec classes
    class ContainerSpec {
        public ArrayList<ContainerSpecInner> containers;
    }
    class ContainerSpecInner {
        public String image;
        public String tag;
        public WithSpec with;
    }
    class WithSpec {
        public String name;
        public ArrayList<Integer> expose;
        public ArrayList<String> cmd;
        public ArrayList<String> volumes;
        public ArrayList<String> volumes_from;
        public ArrayList<String> environment;
        public ArrayList<String> links;
    }


    /**
     *
     * @return
     * @throws Exception
     */
    public List<AbstractContainer> getContainers() throws Exception {
        Gson gsRef = new Gson();
        ContainerSpec cs = gsRef.fromJson(jsonSpec, ContainerSpec.class);
        logger.info(cs);
        ArrayList<AbstractContainer> retList = new ArrayList<>();

        for (ContainerSpecInner o : cs.containers) {
            if (o.image.isEmpty()) {
                throw new Exception("Image name is not specified!");
            }
            if (o.tag.isEmpty()) {
                o.tag = "latest";
            }
            if (o.with.name.isEmpty()) {
                throw new Exception("Container name is not specified!");
            }

            retList.add(new ContainerBuilder(this.dockerClient, o));
        }

        return retList;
    }

}


