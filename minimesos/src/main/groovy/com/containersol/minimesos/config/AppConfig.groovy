package com.containersol.minimesos.config

import com.containersol.minimesos.MinimesosException
import com.containersol.minimesos.cluster.MesosCluster

/**
 * Configuration for a Marathon app. Path is relative to the minimesosFile.
 */
class AppConfig {

    String marathonJsonPath

    String marathonJsonUrl

    private URL url

    private File file

    void setMarathonJsonPath(String marathonJsonPath) {
        if (this.url != null) {
            throw new MinimesosException("Set either 'marathonJsonPath' or 'marathonJsonUrl' but not both")
        }

        File file = new File(MesosCluster.getHostDir(), marathonJsonPath)
        if (!file.exists()) {
            throw new MinimesosException("File in Marathon config does not exist: '" + file.getAbsolutePath() + "'")
        } else {
            this.file = file
        }
    }

    void setMarathonJsonUrl(String urlString) {
        if (this.file != null) {
            throw new MinimesosException("Set either 'marathonJsonPath' or 'marathonJsonUrl' but not both")
        }

        try {
            this.url = new URL(urlString);
        } catch (MalformedURLException ignored) {
            throw new MinimesosException("Malformed URL: " + urlString)
        }
    }

    File getFile() {
        return file
    }

    URL getUrl() {
        return url
    }
}
