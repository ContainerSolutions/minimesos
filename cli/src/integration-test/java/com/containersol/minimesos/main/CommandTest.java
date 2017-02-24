package com.containersol.minimesos.main;

import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandTest {

    private ByteArrayOutputStream outputStream;

    private PrintStream ps;

    private ClusterRepository repository = new ClusterRepository();

    @Before
    public void initTest() {
        outputStream = new ByteArrayOutputStream();
        ps = new PrintStream(outputStream, true);
    }

    @Test
    public void testUpAndDestroy() {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");
        commandUp.execute();

        MesosCluster cluster = commandUp.getCluster();

        File minimesosFile = repository.getMinimesosFile();

        assertTrue("Minimesos file at " + minimesosFile + " should exist", minimesosFile.exists());

        assertEquals(7, cluster.getMemberProcesses().size());

        cluster.destroy(new MesosClusterContainersFactory());

        assertFalse("Minimesos file at " + minimesosFile + " should be removed", minimesosFile.exists());
    }

    @Test
    public void testUp_invalidMinimesosFile() throws IOException {
        FileUtils.write(repository.getMinimesosFile(), "invalid", "UTF-8");

        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");
        commandUp.execute();
        MesosCluster cluster = commandUp.getCluster();

        String fileContent = FileUtils.readFileToString(repository.getMinimesosFile(), "UTF-8");
        assertEquals("Invalid state file has not been overwritten", cluster.getClusterId(), fileContent);

        cluster.destroy(new MesosClusterContainersFactory());

        File minimesosFile = repository.getMinimesosFile();

        assertFalse("Minimesos file at " + minimesosFile + " should be removed", minimesosFile.exists());
    }

    @Test
    public void testUp_alreadyRunning() {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");

        commandUp.execute();
        MesosCluster firstCluster = commandUp.getCluster();

        commandUp.execute();
        MesosCluster secondCluster = commandUp.getCluster();

        assertEquals(firstCluster, secondCluster);

        firstCluster.destroy(new MesosClusterContainersFactory());
        secondCluster.destroy(new MesosClusterContainersFactory());

        File minimesosFile = repository.getMinimesosFile();

        assertFalse("Minimesos file at " + minimesosFile + " should be removed", minimesosFile.exists());
    }

    @Test
    public void testInfo_runningCluster() throws IOException {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");
        commandUp.execute();

        CommandInfo commandInfo = new CommandInfo(ps);
        commandInfo.execute();

        String result = outputStream.toString("UTF-8");

        assertTrue(result.contains("Minimesos cluster is running"));
        assertTrue(result.contains("Mesos version"));

        commandUp.getCluster().destroy(new MesosClusterContainersFactory());
    }

    @Test
    public void testInfo_notRunning() throws IOException {
        CommandInfo commandInfo = new CommandInfo(ps);
        commandInfo.execute();

        String result = outputStream.toString("UTF-8");
        assertTrue("Expected phrase not found in: " + result, result.contains("Cluster ID is not found"));
    }

    @Test
    public void testState() throws IOException {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");
        commandUp.execute();
        MesosCluster cluster = commandUp.getCluster();

        CommandState commandState = new CommandState(ps);
        commandState.execute();

        JSONObject state = new JSONObject(outputStream.toString("UTF-8"));

        assertEquals("master@" + cluster.getMaster().getIpAddress() + ":5050", state.getString("leader"));

        cluster.destroy(new MesosClusterContainersFactory());
    }

    @Test
    public void testInstallCommandValidation() {
        CommandInstall install = new CommandInstall();
        assertFalse("Install command requires one of the parameters", install.validateParameters());
    }

    @Test
    public void testInstall() {
        CommandUp commandUp = new CommandUp();
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");
        commandUp.execute();

        CommandInstall install = new CommandInstall();
        install.setMarathonFile("src/integration-test/resources/app.json");

        install.execute();

        commandUp.getCluster().destroy(new MesosClusterContainersFactory());
    }

    @Test(expected = MinimesosException.class)
    public void testInstall_alreadyRunning() {
        CommandUp commandUp = new CommandUp();
        commandUp.execute();

        CommandInstall install = new CommandInstall();
        install.setMarathonFile("src/integration-test/resources/app.json");

        install.execute();
        install.execute();

        commandUp.getCluster().destroy(new MesosClusterContainersFactory());
    }

    @Test
    public void testState_notRunning() throws IOException {
        CommandState commandState = new CommandState(ps);
        commandState.execute();

        String result = outputStream.toString("UTF-8");
        assertTrue(result.contains("Minimesos cluster is not running"));
    }

    @Test
    public void testCompleteInitFile() throws UnsupportedEncodingException {
        CommandUp commandUp = new CommandUp(ps);
        commandUp.setClusterConfigPath("src/integration-test/resources/configFiles/complete-minimesosFile");
        commandUp.execute();

        String result = outputStream.toString("UTF-8");

        assertTrue("Command up output does not contain expected line", result.contains("Minimesos cluster is running"));
        assertTrue(result.contains("Mesos version"));

        assertTrue(result.contains("MINIMESOS_MARATHON"));

        commandUp.getCluster().destroy(new MesosClusterContainersFactory());
    }

}
