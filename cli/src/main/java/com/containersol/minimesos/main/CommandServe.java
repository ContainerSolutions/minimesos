package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes a default minimesosFile in the directory where minimesos is run
 */
@Parameters(separators = "=", commandDescription = "Start the API Webservice")
public class CommandServe implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandServe.class);

    public static final String CLINAME = "serve";

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

        ApiContainer apiContainer = new ApiContainer();
        apiContainer.start(5);

    }

}
