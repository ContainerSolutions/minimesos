package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;

/**
 * Help command
 */
@Parameters(separators = "=", commandDescription = "Display help")
public class CommandHelp implements Command {

    public static final String CLINAME = "help";

    @Override
    public boolean isExposedHostPorts() {
        return false;
    }

    @Override
    public boolean getStartConsul() {
        return false;
    }

}
