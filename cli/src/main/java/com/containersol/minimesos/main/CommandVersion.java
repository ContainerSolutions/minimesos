package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;

import java.io.PrintStream;

/**
 * Command for printing the minimesos version
 */
@Parameters(separators = "=", commandDescription = "Display the version of minimesos")
public class CommandVersion implements Command {

    private static final String CLI_NAME = "version";

    private PrintStream output = System.out; //NOSONAR

    CommandVersion() { // NOSONAR

    }

    public CommandVersion(PrintStream ps) {
        this.output = ps;
    }

    @Override
    public void execute() {
        Package mainPackage = Main.class.getPackage();
        String version = mainPackage.getImplementationVersion();
        output.println(version);
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLI_NAME;
    }
}
