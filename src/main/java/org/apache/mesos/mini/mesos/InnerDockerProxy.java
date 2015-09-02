package org.apache.mesos.mini.mesos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.PortBinding;
import org.apache.mesos.mini.container.AbstractContainer;

public class InnerDockerProxy extends AbstractContainer {

  private static final String DOCKER_IMAGE = "mwldk/go-tcp-proxy";

  private static final int PROXY_PORT = 2377;

  private MesosContainer mesosContainer;

  public InnerDockerProxy(DockerClient dockerClient, MesosContainer mesosContainer) {
    super(dockerClient);
    this.mesosContainer = mesosContainer;
  }

  @Override
  protected void pullImage() {
    pullImage(DOCKER_IMAGE, "latest");
  }

  @Override
  protected CreateContainerCmd dockerCommand() {
    return dockerClient
        .createContainerCmd(DOCKER_IMAGE)
        .withLinks(Link.parse(mesosContainer.getContainerId() + ":docker"))
        .withExposedPorts(ExposedPort.tcp(mesosContainer.getDockerPort()))
        .withPortBindings(PortBinding.parse("0.0.0.0:" + getProxyPort() + ":" + mesosContainer.getDockerPort()))
        .withCmd("-l=:" + mesosContainer.getDockerPort(), "-r=docker:" + mesosContainer.getDockerPort());
  }

  public int getProxyPort() {
    return PROXY_PORT;
  }
}
