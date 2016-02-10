package com.containersol.minimesos.main;

import com.containersol.minimesos.MesosCluster;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.File;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Test
    public void testUpAndDestroy() {
        CommandUp commandUp = new CommandUp();
        MesosCluster cluster = commandUp.execute();

        File minimesosFile = ClusterRepository.getMinimesosFile();

        assertTrue("Minimesos file at " + minimesosFile + " should exist", minimesosFile.exists());

        assertEquals(4, cluster.getContainers().size());

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();

        assertFalse("Minimesos file at " + minimesosFile + " should be removed", minimesosFile.exists());
    }

    @Test
    public void testUp_invalidMinimesosFile() throws IOException {
        FileUtils.write(ClusterRepository.getMinimesosFile(), "invalid");

        CommandUp commandUp = new CommandUp();
        MesosCluster cluster = commandUp.execute();

        assertNotEquals("Invalid state file has not been overwritten", cluster.getClusterId(), "invalid");

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();

        File minimesosFile = ClusterRepository.getMinimesosFile();

        assertFalse("Minimesos file at " + minimesosFile + " should be removed", minimesosFile.exists());
    }

    @Test
    public void testUp_alreadyRunning() {
        CommandUp commandUp = new CommandUp();
        MesosCluster firstCluster = commandUp.execute();
        MesosCluster secondCluster = commandUp.execute();
        assertEquals(firstCluster, secondCluster);

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();

        File minimesosFile = ClusterRepository.getMinimesosFile();

        assertFalse("Minimesos file at " + minimesosFile + " should be removed", minimesosFile.exists());
    }

    @Ignore
    public void testInfo_runningCluster() throws IOException {
        CommandUp commandUp = new CommandUp();
        commandUp.execute();

        CommandInfo commandInfo = new CommandInfo();
        commandInfo.execute();

//        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator().contains(FileUtils.readFileToString(new File("src/test/resources/info.txt"))));
        assertEquals(systemOutRule.getLogWithNormalizedLineSeparator(), FileUtils.readFileToString(new File("src/test/resources/info.txt")));

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();
    }

    @Ignore
    public void testInfo_notRunning() throws IOException {
        CommandInfo commandInfo = new CommandInfo();
        commandInfo.execute();

        assertTrue(systemOutRule.getLogWithNormalizedLineSeparator().contains(FileUtils.readFileToString(new File("src/test/resources/info-not-running.txt"))));
    }

}
