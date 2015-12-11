package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Parameters for the 'state' command
 * @todo add --pretty
 */
@Parameters(separators = "=", commandDescription = "Display state.json file of a master or an agent")
public class CommandState {

    @Parameter(names = "--agent", description = "Specify an agent to query, otherwise query a master")
    private String agent = "";

    public String getAgent() {
        return agent;
    }

}
