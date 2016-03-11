package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;

import static java.lang.String.format;

/**
 * Initializes a default minimesosFile in the directory where minimesos is run
 */
@Parameters(separators = "=", commandDescription = "Initialize a minimesosFile")
public class CommandInit implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInit.class);

    public static final String CLINAME = "init";

    public static final String DEFAULT_HOST_USERID = "1000";

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

    @Override
    public void execute() throws MinimesosException {
        File minimesosFile = new File(MesosCluster.getHostDir(), ClusterConfig.DEFAULT_CONFIG_FILE);

        if (minimesosFile.exists()) {
            throw new MinimesosException("A minimesosFile already exists in this directory");
        }

        String fileContent = getConfigFileContent();

        Path minimesosPath = Paths.get(minimesosFile.getAbsolutePath());
        try {
            Files.write(minimesosPath, fileContent.getBytes());
        } catch (IOException e) {
            throw new MinimesosException(format("Could not initialize minimesosFile: %s", e.getMessage()), e);
        }

        LOGGER.info("Initialized minimesosFile in this directory");

        try {
            UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
            UserPrincipal owner = lookupService.lookupPrincipalByName(DEFAULT_HOST_USERID);
            Files.setOwner(minimesosPath, owner);
        } catch (IOException e) {
            throw new MinimesosException("NOTE: minimesosFile remains owned by root instead of user ID " + DEFAULT_HOST_USERID + ": " + e.getMessage());
        }

    }

    public String getConfigFileContent() {
        ClusterConfig config = new ClusterConfig();
        config.setClusterName("Change Cluster Name in " + ClusterConfig.DEFAULT_CONFIG_FILE + " file");

        config.setMaster(new MesosMasterConfig());
        config.setZookeeper(new ZooKeeperConfig());
        config.setMarathon(new MarathonConfig());
        config.getAgents().add(new MesosAgentConfig());

        ConfigParser parser = new ConfigParser();
        return parser.toString(config);
    }

}
