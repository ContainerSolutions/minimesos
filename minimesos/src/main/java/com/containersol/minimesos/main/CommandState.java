package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Parameters for the 'state' command
 *
 * TODO: add --pretty
 */
@Parameters(separators = "=", commandDescription = "Display state.json file of a master or an agent")
public class CommandState implements Command {

    public static final String CLINAME = "state";

    @Parameter(names = "--agent", description = "Specify an agent to query, otherwise query a master")
    private String agent = "";

    public String getAgent() {
        return agent;
    }

    @Override
    public boolean isExposedHostPorts() {
        return false;
    }

    @Override
    public boolean getStartConsul() {
        return false;
    }

    public void execute() {

    }
}
