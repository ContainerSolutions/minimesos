package com.containersol.minimesos.cluster;

import com.containersol.minimesos.MinimesosException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Manages persistent information about the minimesos cluster
 */
public class ClusterRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepository.class);

    public static final String MINIMESOS_FILE_PROPERTY = "minimesos.cluster";

    private ClusterRepository() {
    }

    /**
     * Loads representation of the running cluster
     *
     * @return representation of the cluster, which ID is found in the file
     */
    public static MesosCluster loadCluster(MesosClusterFactory factory) {
        String clusterId = ClusterRepository.readClusterId();
        if (clusterId != null) {
            try {
                return MesosCluster.loadCluster(clusterId, factory);
            } catch (MinimesosException e) {
                ClusterRepository.deleteMinimesosFile();
            }
        }
        return null;
    }

    /**
     * Writes cluster id to file
     *
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

    public static String readClusterId() {
        try {
            File minimesosFile = getMinimesosFile();
            String clusterId = FileUtils.readFileToString(minimesosFile);
            LOGGER.debug("Reading cluster ID from " + minimesosFile + ": " + clusterId);
            return clusterId;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @return file, possibly non-existing, where cluster information is stored
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
