package org.apache.mesos.mini;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.PortBinding;
import org.apache.mesos.mini.util.DockerUtil;

public class DockerProxy {
    private final DockerClient dockerClient;
    private final DockerUtil dockerUtil;

    public DockerProxy(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        dockerUtil = new DockerUtil(dockerClient);
    }

    public String startProxy() {
        dockerUtil.pullImage("paintedfox/tinyproxy", "latest");
        CreateContainerCmd command = dockerClient.createContainerCmd("paintedfox/tinyproxy").withPortBindings(PortBinding.parse("0.0.0.0:8888:8888"));
        return dockerUtil.createAndStart(command);
    }
}