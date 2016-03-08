package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandInitTest {

    private CommandInit commandInit;

    @Before
    public void before() {
        commandInit = new CommandInit();
    }

    @Test
    public void testFileContent() throws IOException {
        String fileContent = commandInit.getConfigFileContent();
        assertTrue("agent section is not found", fileContent.contains("agent {"));
        assertTrue("agent resources section is not found", fileContent.contains("resources {"));
        assertTrue("zookeeper section is not found", fileContent.contains("zookeeper {"));
    }

    @Test(expected = MinimesosException.class)
    public void testExecute_existingMiniMesosFile() throws IOException {

        String oldHostDir = System.getProperty(MesosCluster.MINIMESOS_HOST_DIR_PROPERTY);
        File dir = File.createTempFile("mimimesos-test", "dir");
        assertTrue("Failed to delete temp file", dir.delete());
        assertTrue("Failed to create temp directory", dir.mkdir());
        System.setProperty(MesosCluster.MINIMESOS_HOST_DIR_PROPERTY, dir.getAbsolutePath());


        File minimesosFile = new File(dir, ClusterConfig.DEFAULT_CONFIG_FILE);
        Files.write(Paths.get(minimesosFile.getAbsolutePath()), "minimesos { }".getBytes());

        try {
            commandInit.execute();
        } finally {
            if (oldHostDir == null) {
                System.getProperties().remove(MesosCluster.MINIMESOS_HOST_DIR_PROPERTY);
            } else {
                System.setProperty(MesosCluster.MINIMESOS_HOST_DIR_PROPERTY, oldHostDir);
            }
            FileUtils.forceDelete(dir);
        }
    }

    @Test
    public void testValidateParameters() {
        assertTrue(commandInit.validateParameters());
    }

    @Test
    public void testName() {
        assertEquals("init", commandInit.getName());
    }

}
