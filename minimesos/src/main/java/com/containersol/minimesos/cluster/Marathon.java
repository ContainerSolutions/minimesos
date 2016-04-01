package com.containersol.minimesos.cluster;

/**
 * Functionality, which is expected from Marathon
 */
public interface Marathon extends MesosContainer {

    /**
     * If Marathon configuration requires, installs the applications
     */
    void installMarathonApps();

    /**
     * Deploys a Marathon app by JSON string
     *
     * @param marathonJson JSON string
     */
    void deployApp(String marathonJson);

    /**
     * Kill all apps that are currently running.
     */
    void killAllApps();

}
