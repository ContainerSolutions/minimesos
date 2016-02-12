package com.containersol.minimesos.cluster;

import com.containersol.minimesos.MinimesosException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClusterRepository {

    private static Logger LOGGER = Logger.getLogger(ClusterRepository.class);

    public static final String MINIMESOS_FILE_PROPERTY = "minimesos.cluster";

    private ClusterRepository() {}

    /**
     * @return representation of the cluster, which ID is found in the file
     */
    public static MesosCluster loadCluster() {
        String clusterId = ClusterRepository.readClusterId();
        if (clusterId != null) {
            try {
                return MesosCluster.loadCluster(clusterId);
            } catch (RuntimeException e) {
                ClusterRepository.deleteMinimesosFile();
            }
        }
        return null;
    }

    /**
     * Writes cluster id to file
     * @param cluster cluster to store ID
     */
    public static void saveClusterFile(MesosCluster cluster) {
        String clusterId = cluster.getClusterId();
        File dotMinimesosDir = ClusterRepository.getMinimesosDir();
        try {
            FileUtils.forceMkdir(dotMinimesosDir);
            String clusterIdPath = dotMinimesosDir.getAbsolutePath() + "/" + MINIMESOS_FILE_PROPERTY;
            Files.write(Paths.get(clusterIdPath), clusterId.getBytes());
            LOGGER.debug("Writing cluster ID " + clusterId + " to " + clusterIdPath);
        } catch (IOException ie) {
            LOGGER.error("Could not write .minimesos folder", ie);
            throw new RuntimeException(ie);
        }
    }

    /**
     * Deletes cluster file
     */
    public static void deleteClusterFile() {
        ClusterRepository.deleteMinimesosFile();
    }

    private static String readClusterId() {
        try {
            String clusterId = IOUtils.toString(new FileReader(getMinimesosFile()));
            LOGGER.debug("Reading cluster ID from " + getMinimesosFile() + ": " + clusterId);
            return clusterId;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @return never null
     */
    public static File getMinimesosFile() {
        return new File(getMinimesosDir(), MINIMESOS_FILE_PROPERTY);
    }

    /**
     * @return directory, where minimesos stores ID file
     */
    public static File getMinimesosDir() {
        File hostDir = MesosCluster.getHostDir();
        File minimesosDir = new File(hostDir, ".minimesos");
        if (!minimesosDir.exists()) {
            if (!minimesosDir.mkdirs()) {
                throw new MinimesosException("Failed to create " + minimesosDir.getAbsolutePath() + " directory");
            }
        }

        return minimesosDir;
    }

    private static void deleteMinimesosFile() {
        File minimesosFile = getMinimesosFile();
        LOGGER.debug("Deleting minimesos.cluster file at " + getMinimesosFile());
        if (minimesosFile.exists()) {
            try {
                FileUtils.forceDelete(minimesosFile);
            } catch (IOException e) {
                // ignore
            }
        }
    }

}