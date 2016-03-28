package com.containersol.minimesos.config

import com.containersol.minimesos.MinimesosException
import com.containersol.minimesos.cluster.MesosCluster

/**
 * Configuration for a Marathon app. Path is relative to the minimesosFile.
 */
class AppConfig {

    String marathonJsonPath

    private File marathonJsonFile

    void setMarathonJsonPath(String marathonJsonPath) {
        File file = new File(MesosCluster.getHostDir(), marathonJsonPath)
        if (!file.exists()) {
            throw new MinimesosException("File in Marathon config does not exist: '" + file.getAbsolutePath() + "'");
        } else {
            this.marathonJsonFile = file
        }
    }

    File getMarathonJsonFile() {
        return marathonJsonFile
    }
}
