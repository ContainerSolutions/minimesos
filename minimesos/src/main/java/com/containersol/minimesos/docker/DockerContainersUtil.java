package com.containersol.minimesos.docker;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Filters;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable utility class, which represents set of docker containers with filters and operations on this list
 */
public class DockerContainersUtil {

    private final DockerClient dockerClient;
    private final Set<Container> containers;

    public DockerContainersUtil(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        this.containers = null;
    }

    private DockerContainersUtil(DockerClient dockerClient, Set<Container> containers) {
        this.dockerClient = dockerClient;
        this.containers = containers;
    }

    /**
     * @param showAll should the list include stopped containers
     * @return set of docker containers
     */
    public DockerContainersUtil getContainers(boolean showAll) {
        Set<Container> containers = new HashSet<>(dockerClient.listContainersCmd().withShowAll(showAll).exec());
        return new DockerContainersUtil( dockerClient, containers );
    }

    public int size() {
        return (containers != null) ? containers.size() : 0;
    }

    /**
     * Filters the set based on the constainer name
     * @param pattern regular expression pattern of the container name
     * @return filtered set
     */
    public DockerContainersUtil filterByName( String pattern ) {

        if( this.containers == null ) {
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

        return new DockerContainersUtil(dockerClient, matched);
    }

    /**
     * Filters the set based on the constainer name
     * @param pattern regular expression pattern of the container name
     * @return filtered set
     */
    public DockerContainersUtil filterByImage( String pattern ) {

        if( this.containers == null ) {
            return this;
        }

        Set<Container> matched = new HashSet<>();
        for (Container container : containers) {
            if( container.getImage().matches(pattern) ) {
                matched.add( container );
            }
        }

        return new DockerContainersUtil(dockerClient, matched);
    }

    /**
     * Removes all containers in the util object
     */
    public void remove() {
        if( containers != null ) {
            for (Container container : containers) {
                dockerClient.removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
            }
        }
    }

    /**
     * Removes all containers in the util object
     */
    public DockerContainersUtil kill() {
        if( containers != null ) {
            for (Container container : containers) {
                dockerClient.killContainerCmd(container.getId()).exec();
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
                ips.add(getIpAddress(dockerClient, container.getId()));
            }
        }
        return ips;
    }

    /**
     * @param dockerClient docker client to use
     * @param containerId  id of the container to inspect
     * @return IP Address of the container
     */
    public static String getIpAddress(DockerClient dockerClient, String containerId) {
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        return response.getNetworkSettings().getIpAddress();
    }

    /**
     * @param dockerClient docker client to use
     * @param containerId id of the container to retrieve
     * @return container or null
     */
    public static Container getContainer(DockerClient dockerClient, String containerId) {
        List<Container> containers = dockerClient.listContainersCmd().withFilters( new Filters().withFilter("id", containerId)).exec();
        if( containers != null && containers.size() == 1) {
            return containers.get(0);
        } else {
            return null;
        }
    }

}