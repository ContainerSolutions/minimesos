package com.containersol.minimesos.main;

import com.containersol.minimesos.MesosCluster;
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

    public static void saveCluster(MesosCluster cluster) {
        String clusterId = cluster.getClusterId();
        File dotMinimesosDir = ClusterRepository.getDotMinimesosDir();
        try {
            FileUtils.forceMkdir(dotMinimesosDir);
            String clusterIdPath = dotMinimesosDir.getAbsolutePath() + "/" + MINIMESOS_FILE_PROPERTY;
            Files.write(Paths.get(clusterIdPath), clusterId.getBytes());
            LOGGER.info("Writing cluster ID " + clusterId + " to " + clusterIdPath);
        } catch (IOException ie) {
            LOGGER.error("Could not write .minimesos folder", ie);
            throw new RuntimeException(ie);
        }
    }

    public static void deleteCluster() {
        ClusterRepository.deleteMinimesosFile();
    }

    private static String readClusterId() {
        try {
            String clusterId = IOUtils.toString(new FileReader(getMinimesosFile()));
            LOGGER.info("Reading cluster ID from " + getMinimesosFile() + ": " + clusterId);
            return clusterId;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @return never null
     */
    public static File getMinimesosFile() {
        return new File(getDotMinimesosDir(), MINIMESOS_FILE_PROPERTY);
    }

    public static File getDotMinimesosDir() {
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
        LOGGER.info("Deleting minimesos.cluster file at " + getMinimesosFile());
        if (minimesosFile.exists()) {
            try {
                FileUtils.forceDelete(minimesosFile);
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
