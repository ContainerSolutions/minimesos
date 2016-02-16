package com.containersol.minimesos.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Main method for interacting with minimesos.
 */
@Parameters(separators = "=", commandDescription = "Global options")
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Parameter(names = {"--help", "-help", "-?", "-h"}, description = "Show help")
    private boolean help = false;

    private PrintStream output = System.out;

    private final JCommander jc;

    private HashMap<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {
        Main main = new Main();
        main.addCommand(new CommandUp());
        main.addCommand(new CommandDestroy());
        main.addCommand(new CommandHelp());
        main.addCommand(new CommandInstall());
        main.addCommand(new CommandState());
        main.addCommand(new CommandInfo());
        main.run(args);
    }

    public Main() {
        jc = new JCommander(this);
        jc.setProgramName("minimesos");
    }

    public void setOutput(PrintStream output) {
        this.output = output;
    }

    public void run(String[] args) {

        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            jc.addCommand(entry.getKey(), entry.getValue());
        }

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse parameters. " + e.getMessage() + "\n");
            printUsage(null);
            return;
        }

        if (jc.getParameters().get(0).isAssigned()) {
            printUsage(null);
            return;
        }

        if (jc.getParsedCommand() == null) {
            MesosCluster cluster = ClusterRepository.loadCluster();
            if (cluster != null) {
                cluster.printServiceUrls(output);
            } else {
                printUsage(null);
            }
            return;
        }

        Command parsedCommand = commands.get(jc.getParsedCommand());

        if (parsedCommand == null) {
            LOGGER.error("No such command: " + jc.getParsedCommand());
        } else if (CommandHelp.CLINAME.equals(parsedCommand.getName())) {
            printUsage(null);
        } else {
            if (parsedCommand.validateParameters()) {
                parsedCommand.execute();
            } else {
                printUsage(jc.getParsedCommand());
            }
        }

    }

    private void printUsage(String commandName) {
        StringBuilder builder = new StringBuilder();
        if (commandName != null) {
            jc.usage(commandName, builder);
        } else {
            jc.usage(builder);
        }
        output.println(builder.toString());
    }

    public void addCommand(Command command) {
        commands.put(command.getName(), command);
    }

}
