package com.containersol.minimesos.main;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
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

    private static final int EXIT_CODE_OK = 0;
    private static final int EXIT_CODE_ERR = 1;

    @Parameter(names = {"--help", "-help", "-?", "-h"}, description = "Show help")
    private boolean help = false;

    @Parameter(names = "--debug", description = "Enable debug logging.")
    private Boolean debug = null;

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
        main.addCommand(new CommandInit());
        try {
            int rc = main.run(args);
            if (EXIT_CODE_OK != rc) {
                System.exit(rc);
            }
        } catch (MinimesosException mme) {
            if (main.debug) {
                LOGGER.error("An error, which was handled, occurred", mme);
            } else {
                LOGGER.error(mme.getMessage());
            }
            System.exit(EXIT_CODE_ERR);
        }
    }

    public Main() {
        jc = new JCommander(this);
        jc.setProgramName("minimesos");
    }

    public void setOutput(PrintStream output) {
        this.output = output;
    }

    public int run(String[] args) {

        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            jc.addCommand(entry.getKey(), entry.getValue());
        }

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse parameters. " + e.getMessage() + "\n");
            printUsage(null);
            return EXIT_CODE_ERR;
        }

        if (help) {
            printUsage(null);
            return EXIT_CODE_OK;
        }

        if (debug != null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("com.containersol.minimesos.container");
            rootLogger.setLevel(Level.DEBUG);
            LOGGER.debug("Initialized debug logging");
        }

        if (jc.getParsedCommand() == null) {
            MesosCluster cluster = ClusterRepository.loadCluster(new MesosClusterContainersFactory());
            if (cluster != null) {
                new CommandInfo().execute();
                return EXIT_CODE_OK;
            } else {
                printUsage(null);
                return EXIT_CODE_ERR;
            }
        }

        Command parsedCommand = commands.get(jc.getParsedCommand());

        if (parsedCommand == null) {
            LOGGER.error("No such command: " + jc.getParsedCommand());
            return EXIT_CODE_ERR;
        } else if (CommandHelp.CLINAME.equals(parsedCommand.getName())) {
            printUsage(null);
        } else {
            if (parsedCommand.validateParameters()) {
                parsedCommand.execute();
            } else {
                printUsage(jc.getParsedCommand());
                return EXIT_CODE_ERR;
            }
        }

        return EXIT_CODE_OK;
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
