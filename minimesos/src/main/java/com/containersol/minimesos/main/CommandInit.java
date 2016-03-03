package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.MesosCluster;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.String.format;

/**
 * Initializes a default minimesosFile in the directory where minimesos is run
 */
@Parameters(separators = "=", commandDescription = "Initialize a minimesosFile")
public class CommandInit implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandInit.class);

    public static final String CLINAME = "init";

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
        File minimesosFile = new File(MesosCluster.getHostDir(), "minimesosFile");

        if (minimesosFile.exists()) {
            throw new MinimesosException("A minimesosFile already exists in this directory");
        }

        try (InputStream minimesosFileTemplate = getClass().getResourceAsStream("/minimesosFile")) {
            Files.write(Paths.get(minimesosFile.getAbsolutePath()), IOUtils.toString(minimesosFileTemplate).getBytes());
            LOGGER.info("Initialized minimesosFile in this directory");
        } catch (IOException e) {
            throw new MinimesosException(format("Could not initialize minimesosFile: %s", e.getMessage()), e);
        }
    }
}
