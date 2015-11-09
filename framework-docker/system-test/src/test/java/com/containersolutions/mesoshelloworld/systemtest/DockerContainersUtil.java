package com.containersolutions.mesoshelloworld.systemtest;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;

import java.util.HashSet;
import java.util.Set;

/**
 * Immutable utility class, which represents set of docker containers with filters and operations on this list
 * TODO: delete this class when minimesos 0.5.0 is released. It will be present there
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
                dockerClient.removeContainerCmd(container.getId()).exec();
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
        if( containers != null ) {
            for (Container container : containers) {
                InspectContainerResponse response = dockerClient.inspectContainerCmd(container.getId()).exec();
                ips.add( response.getNetworkSettings().getIpAddress() );
            }
        }
        return ips;
    }

}
