package com.containersol.minimesos.docker;

import com.containersol.minimesos.MinimesosException;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Immutable utility class, which represents set of docker containers with filters and operations on this list
 */
public class DockerContainersUtil {

    private final List<Container> containers;

    private DockerContainersUtil(List<Container> containers) {
        this.containers = containers;
    }

    /**
     * Use this getter if you need to iterate over docker objects
     *
     * @return set of docker containers
     */
    public List<Container> getContainers() {
        return containers;
    }

    /**
     * @param showAll should the list include stopped containers
     * @return set of docker containers
     */
    public static DockerContainersUtil getContainers(boolean showAll) {
        List<Container> newContainers = new ArrayList<>(DockerClientFactory.build().listContainersCmd().withShowAll(showAll).exec());
        return new DockerContainersUtil(newContainers);
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

        List<Container> matched = new ArrayList<>();
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

        List<Container> matched = new ArrayList<>();
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
                DockerClientFactory.build().removeContainerCmd(container.getId()).withForce(true).withRemoveVolumes(true).exec();
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
                    DockerClientFactory.build().killContainerCmd(container.getId()).exec();
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
        InspectContainerResponse response = DockerClientFactory.build().inspectContainerCmd(containerId).exec();
        return response.getNetworkSettings().getIpAddress();
    }

    /**
     * Synchronized method for returning logs of docker container
     *
     * @param containerId - ID of the container ot lookup logs
     * @return list of strings, where every string is log line
     */
    public static List<String> getDockerLogs(String containerId) {

        final List<String> logs = new ArrayList<>();

        LogContainerCmd logContainerCmd = DockerClientFactory.build().logContainerCmd(containerId);
        logContainerCmd.withStdOut(true).withStdErr(true);
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
     * Synchronized method for pulling docker image
     *
     * @param imageName    image to pull
     * @param imageVersion image version to pull
     * @param timeoutSecs  pulling timeout in seconds
     */
    public static void pullImage(String imageName, String imageVersion, long timeoutSecs) {

        final CompletableFuture<Void> result = new CompletableFuture<>();

        try {
            DockerClientFactory.build().pullImageCmd(imageName).withTag(imageVersion).exec(new PullImageResultCallback() {

                @Override
                public void onNext(PullResponseItem item) {
                    String status = item.getStatus();
                    if (status == null) {
                        result.completeExceptionally(new MinimesosException("docker failed to pull image"));
                    }
                }

                @Override
                public void onComplete() {
                    super.onComplete();
                    result.complete(null);
                }

            });
        } catch (RuntimeException rte) {
            throw new MinimesosException(String.format("Failed to pull %s:%s container", imageName, imageVersion), rte);
        }

        try {
            result.get(timeoutSecs, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            // we get here on future.completeExceptionally() above
            String msg = String.format("# Error pulling image from registry. Try executing the command below manually%ndocker pull %s:%s", imageName, imageVersion);
            throw new MinimesosException(msg, e);
        } catch (InterruptedException | TimeoutException | RuntimeException e) {
            String msg = "Error pulling image or image not found in registry: " + imageName + ":" + imageVersion;
            throw new MinimesosException(msg, e);
        }

    }

    /**
     * @return IP Address of the container's gateway (which would be docker0)
     */
    public static String getGatewayIpAddress() {
        List<Container> containers = DockerClientFactory.build().listContainersCmd().exec();
        if (containers == null || containers.size() == 0) {
            throw new IllegalStateException("Cannot get docker0 IP address because no containers are running");
        }

        InspectContainerResponse response = DockerClientFactory.build().inspectContainerCmd(containers.get(0).getId()).exec();
        return response.getNetworkSettings().getGateway();
    }

    /**
     * @param containerId id of the container to retrieve
     * @return container or null
     */
    public static Container getContainer(String containerId) {
        List<Container> containers = DockerClientFactory.build().listContainersCmd().withShowAll(true).exec();
        Container container = null;
        if (containers != null && !containers.isEmpty()) {
            Optional<Container> optional = containers.stream().filter(c -> c.getId().equals(containerId)).findFirst();
            if (optional.isPresent()) {
                container = optional.get();
            }
        }
        return container;
    }

}
