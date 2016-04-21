package com.containersol.minimesos.container;

/**
 * Utility class to assist container naming convention
 */
public class ContainerName {

    // disable creation on instances
    private ContainerName() {
    }

    public static String getContainerNamePattern(String clusterId) {
        return "^minimesos-\\w+-" + clusterId + "-\\w+$";
    }

    /**
     * @param container to build the name for
     * @return name of the container
     */
    public static String get(AbstractContainer container) {
        return String.format("minimesos-%s-%s-%s", container.getRole(), container.getClusterId(), container.getUuid());
    }

    /**
     * Based on container name check if it has the given role in the cluster
     *
     * @param containerName to analyse
     * @param clusterId     cluster to check
     * @param role          role to check
     * @return true if container has the role
     */
    public static boolean hasRoleInCluster(String containerName, String clusterId, String role) {
        String expectedStart = String.format("minimesos-%s-%s-", role, clusterId);
        return containerName.startsWith(expectedStart);
    }

    /**
     * Based on container name check if it has the given role in the cluster
     *
     * @param dockerNames as returned by <code>container.getNames()</code>
     * @param clusterId   cluster to check
     * @param role        role to check
     * @return true if container has the role
     */
    public static boolean hasRoleInCluster(String[] dockerNames, String clusterId, String role) {
        String name = getFromDockerNames(dockerNames);
        return hasRoleInCluster(name, clusterId, role);
    }

    /**
     * @return true, if container with this name belongs to the cluster
     */
    public static boolean belongsToCluster(String containerName, String clusterId) {
        String pattern = getContainerNamePattern(clusterId);
        return containerName.matches(pattern);
    }

    /**
     * @return true, if container with these docker names belongs to the cluster
     */
    public static boolean belongsToCluster(String[] dockerNames, String clusterId) {
        String name = getFromDockerNames(dockerNames);
        return belongsToCluster(name, clusterId);
    }

    /**
     * Docker supports multiple names for a single container, when the container is linked from others.
     * This method selects the original name of the container and removes leading "/"
     *
     * @param dockerNames names, as they returned by <code>container.getNames()</code>
     * @return name of the container, which is not inherited from link
     */
    public static String getFromDockerNames(String[] dockerNames) {
        String name = null;
        for (String dockerName : dockerNames) {
            String slashLess = dockerName;
            if (dockerName.startsWith("/")) {
                slashLess = dockerName.substring(1);
            }
            if (!slashLess.contains("/")) {
                name = slashLess;
                break;
            }
        }
        return name;
    }

}
