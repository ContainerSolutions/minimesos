package com.containersol.minimesos.main;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

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
    private boolean debug = false;

    private PrintStream output = System.out; //NOSONAR

    private final JCommander jc = new JCommander(this);

    private HashMap<String, Command> commands = new HashMap<>();

    private ClusterRepository repository = new ClusterRepository();

    public static void main(String[] args) {
        Main main = new Main();
        main.addCommand(new CommandUp());
        main.addCommand(new CommandDestroy());
        main.addCommand(new CommandHelp());
        main.addCommand(new CommandInstall());
        main.addCommand(new CommandUninstall());
        main.addCommand(new CommandState());
        main.addCommand(new CommandInfo());
        main.addCommand(new CommandInit());
        main.addCommand(new CommandPs());
        main.addCommand(new CommandVersion());
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


    public void setOutput(PrintStream output) {
        this.output = output;
    }

    public int run(String[] args) {
        initJCommander();

        try {
            parseParams(jc, args);

            if (help) {
                printUsage(null);
                return EXIT_CODE_OK;
            }

            if (debug) {
                initializeDebugLogging();
            }

            if (jc.getParsedCommand() == null) {
                return handleNoCommand();
            }

            if (!commands.containsKey(jc.getParsedCommand())) {
                LOGGER.error("Unknown command: " + jc.getParsedCommand());
                return EXIT_CODE_ERR;
            }

            Command parsedCommand = commands.get(jc.getParsedCommand());
            if (CommandHelp.CLINAME.equals(parsedCommand.getName())) {
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
        } catch (Exception ex) {
            if (ex.getMessage() != null) {
                output.println("Failed to run command '" + jc.getParsedCommand() + "'. " + ex.getMessage());
            } else {
                output.println("Failed to run command '" + jc.getParsedCommand() + "'.");
                ex.printStackTrace();
            }
            LOGGER.debug("Exception while processing", ex);
            return EXIT_CODE_ERR;
        }
    }

    private JCommander initJCommander() {
        jc.setProgramName("minimesos");
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            jc.addCommand(entry.getKey(), entry.getValue());
        }
        return jc;
    }

    private void parseParams(JCommander jc, String[] args) {
        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse parameters. " + e.getMessage() + "\n");
            printUsage(null);
            throw e;
        }
    }

    private static void initializeDebugLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = (
            loggerContext.getLogger("com.containersol.minimesos")
        );
        rootLogger.setLevel(Level.DEBUG);
        LOGGER.debug("Initialized debug logging");
    }

    private int handleNoCommand() {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());
        if (cluster != null) {
            new CommandInfo().execute();
            return EXIT_CODE_OK;
        } else {
            printUsage(null);
            return EXIT_CODE_ERR;
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
