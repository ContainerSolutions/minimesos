package com.containersol.minimesos.main;

import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CommandTest {

    private ByteArrayOutputStream outputStream;
    private PrintStream ps;

    @Before
    public void initTest() {
        outputStream = new ByteArrayOutputStream();
        ps = new PrintStream(outputStream, true);
    }

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

        String fileContent = FileUtils.readFileToString( ClusterRepository.getMinimesosFile() );
        assertEquals("Invalid state file has not been overwritten", cluster.getClusterId(), fileContent);

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

    @Test
    public void testInfo_runningCluster() throws IOException {
        CommandUp commandUp = new CommandUp();
        commandUp.execute();

        CommandInfo commandInfo = new CommandInfo(ps);
        commandInfo.execute();

        String result = outputStream.toString();
        assertTrue(result.contains(FileUtils.readFileToString(new File("src/test/resources/info.txt"))));

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();
    }

    @Test
    public void testInfo_notRunning() throws IOException {
        CommandInfo commandInfo = new CommandInfo(ps);
        commandInfo.execute();

        String result = outputStream.toString();
        assertTrue(result.contains(FileUtils.readFileToString(new File("src/test/resources/info-not-running.txt"))));
    }

    @Test
    public void testState() throws IOException {
        CommandUp commandUp = new CommandUp();
        MesosCluster cluster = commandUp.execute();

        CommandState commandState = new CommandState(ps);
        commandState.execute();

        JSONObject state = new JSONObject( outputStream.toString() );

        assertEquals("master@" + cluster.getMasterContainer().getIpAddress() + ":5050", state.getString("leader"));

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();
    }

    @Test
    public void testState_notRunning() throws IOException {
        CommandState commandState = new CommandState(ps);
        commandState.execute();

        String result = outputStream.toString();
        assertTrue(result.contains("Minimesos cluster is not running"));

        CommandDestroy commandDestroy = new CommandDestroy();
        commandDestroy.execute();
    }

}
