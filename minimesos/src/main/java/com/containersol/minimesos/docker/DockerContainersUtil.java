package com.containersol.minimesos.docker;


import com.containersol.minimesos.MinimesosException;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Filters;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable utility class, which represents set of docker containers with filters and operations on this list
 */
public class DockerContainersUtil {
    private final Set<Container> containers;

    public DockerContainersUtil() {
        this.containers = null;
    }

    private DockerContainersUtil(Set<Container> containers) {
        this.containers = containers;
    }

    /**
     * Use this getter if you need to iterate over docker objects
     * @return set of docker containers
     */
    public Set<Container> getContainers() {
        return containers;
    }

    /**
     * @param showAll should the list include stopped containers
     * @return set of docker containers
     */
    public DockerContainersUtil getContainers(boolean showAll) {
        Set<Container> containers = new HashSet<>(DockerClientFactory.getDockerClient().listContainersCmd().withShowAll(showAll).exec());
        return new DockerContainersUtil(containers);
    }

    public int size() {
        return (containers != null) ? containers.size() : 0;
    }

    /**
     * Filters the set based on the container name
     *
     * @param pattern regular expression pattern of the container name
     * @return filtered set
     */
    public DockerContainersUtil filterByName(String pattern) {
        if (this.containers == null) {
            return this;
        }

        Set<Container> matched = new HashSet<>();
        for (Container container : containers) {
            String[] names = container.getNames();
            for (String name : names) {
                // all names start with '/'
                if (name.substring(1).matches(pattern)) {
                    matched.add(container);
                }
            }
        }

        return new DockerContainersUtil(matched);
    }

    /**
     * Filters the set based on the constainer name
     *
     * @param pattern regular expression pattern of the container name
     * @return filtered set
     */
    public DockerContainersUtil filterByImage(String pattern) {
        if (this.containers == null) {
            return this;
        }

        Set<Container> matched = new HashSet<>();
        for (Container container : containers) {
            if (container.getImage().matches(pattern)) {
                matched.add(container);
            }
        }

        return new DockerContainersUtil(matched);
    }

    /**
     * Removes all containers in the util object
     */
    public void remove() {
        if (containers != null) {
            for (Container container : containers) {
                DockerClientFactory.getDockerClient().removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
            }
        }
    }

    /**
     * Removes all containers in the util object
     */
    public DockerContainersUtil kill() {
        return kill(false);
    }

    /**
     * Removes all containers in the util object
     *
     * @param ignoreFailure - use <code>true</code> if you expect containers might be stopped by this time
     */
    public DockerContainersUtil kill(boolean ignoreFailure) {
        if (containers != null) {
            for (Container container : containers) {
                try {
                    DockerClientFactory.getDockerClient().killContainerCmd(container.getId()).exec();
                } catch (DockerException failure) {
                    if (!ignoreFailure) {
                        throw failure;
                    }
                }
            }
        }
        return this;
    }

    /**
     * @return IP addresses of containers
     */
    public Set<String> getIpAddresses() {
        Set<String> ips = new HashSet<>();
        if (containers != null) {
            for (Container container : containers) {
                ips.add(getIpAddress(container.getId()));
            }
        }
        return ips;
    }

    /**
     * @param containerId id of the container to inspect
     * @return IP Address of the container
     */
    public static String getIpAddress(String containerId) {
        InspectContainerResponse response = DockerClientFactory.getDockerClient().inspectContainerCmd(containerId).exec();
        return response.getNetworkSettings().getIpAddress();
    }

    public static List<String> getDockerLogs(String containerId) {

        final List<String> logs = new ArrayList<>();

        LogContainerCmd logContainerCmd = DockerClientFactory.getDockerClient().logContainerCmd(containerId);
        logContainerCmd.withStdOut().withStdErr();
        try {
            logContainerCmd.exec(new LogContainerResultCallback() {
                @Override
                public void onNext(Frame item) {
                    logs.add(item.toString());
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new MinimesosException("Failed to retrieve logs of container " + containerId, e);
        }

        return logs;
    }

    /**
     * @return IP Address of the container's gateway (which would be docker0)
     */
    public static String getGatewayIpAddress() {
        List<Container> containers = DockerClientFactory.getDockerClient().listContainersCmd().exec();
        if (containers == null || containers.size() == 0) {
            throw new IllegalStateException("Cannot get docker0 IP address because no containers are running");
        }

        InspectContainerResponse response = DockerClientFactory.getDockerClient().inspectContainerCmd(containers.get(0).getId()).exec();
        return response.getNetworkSettings().getGateway();
    }

    /**
     * @param containerId id of the container to retrieve
     * @return container or null
     */
    public static Container getContainer(String containerId) {
        List<Container> containers = DockerClientFactory.getDockerClient().listContainersCmd().withFilters(new Filters().withFilter("id", containerId)).exec();
        if (containers != null && containers.size() == 1) {
            return containers.get(0);
        } else {
            return null;
        }
    }

}
