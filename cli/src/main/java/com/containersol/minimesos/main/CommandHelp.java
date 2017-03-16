package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;

/**
 * Help command
 */
@Parameters(separators = "=", commandDescription = "Display help")
public class CommandHelp implements Command {

    static final String CLINAME = "help";

    @Override
    public void execute() {
        // Usage is being printed from Main
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

}
