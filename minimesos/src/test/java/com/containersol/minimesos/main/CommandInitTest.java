package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
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
    public void testExecute() throws IOException {
        File minimesosFile = new File("minimesosFile");
        try {
            commandInit.execute();
            assertTrue(minimesosFile.exists());
            assertEquals(IOUtils.toString(getClass().getResourceAsStream("/minimesosFile")), IOUtils.toString(new FileReader(minimesosFile)));
        } finally {
            FileUtils.forceDelete(minimesosFile);
        }
    }

    @Test(expected = MinimesosException.class)
    public void testExecute_existingMiniMesosFile() throws IOException {
        File minimesosFile = new File(MesosCluster.getHostDir(), "minimesosFile");
        Files.write(Paths.get(minimesosFile.getAbsolutePath()), "minimesos { }".getBytes());

        try {
            commandInit.execute();
        } finally {
            FileUtils.forceDelete(minimesosFile);
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
