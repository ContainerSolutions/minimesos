package com.containersol.minimesos.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.AppConfig;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.config.MarathonConfig;
import com.containersol.minimesos.config.MesosAgentConfig;
import com.containersol.minimesos.config.MesosDNSConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.config.RegistratorConfig;
import com.containersol.minimesos.config.ZooKeeperConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void execute() {
        File minimesosFile = new File(MesosCluster.getClusterHostDir(), ClusterConfig.DEFAULT_CONFIG_FILE);

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
            throw new MinimesosException("NOTE: minimesosFile remains owned by root instead of user ID " + DEFAULT_HOST_USERID + ": " + e.getMessage(), e);
        }

    }

    public String getConfigFileContent() {
        ClusterConfig config = new ClusterConfig();
        config.setClusterName("Change Cluster Name in " + ClusterConfig.DEFAULT_CONFIG_FILE + " file");

        config.setMaster(new MesosMasterConfig(ClusterConfig.DEFAULT_MESOS_VERSION));
        config.setZookeeper(new ZooKeeperConfig());
        config.getAgents().add(new MesosAgentConfig(ClusterConfig.DEFAULT_MESOS_VERSION));
        config.setConsul(new ConsulConfig());
        config.setRegistrator(new RegistratorConfig());
        config.setMesosdns(new MesosDNSConfig());

        AppConfig weaveConfig = new AppConfig();
        weaveConfig.setMarathonJson("https://raw.githubusercontent.com/ContainerSolutions/minimesos/master/opt/apps/weave-scope.json");

        MarathonConfig marathonConfig = new MarathonConfig();
        marathonConfig.getApps().add(weaveConfig);
        config.setMarathon(marathonConfig);

        ConfigParser parser = new ConfigParser();
        return parser.toString(config);
    }

}
