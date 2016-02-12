package com.containersol.minimesos.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main method for interacting with minimesos.
 */
@Parameters(separators = "=", commandDescription = "Global options")
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Parameter(names = {"--help", "-help", "-?", "-h"}, description = "Show help")
    private boolean help = false;

    private final JCommander jc;

    private CommandUp commandUp;
    private CommandDestroy commandDestroy;
    private CommandHelp commandHelp;
    private CommandInfo commandInfo;
    private CommandInstall commandInstall;
    private CommandState commandState;

    public static void main(String[] args) {
        Main main = new Main();
        main.setCommandUp(new CommandUp());
        main.setCommandDestroy(new CommandDestroy());
        main.setCommandHelp(new CommandHelp());
        main.setCommandInstall(new CommandInstall());
        main.setCommandState(new CommandState());
        main.setCommandInfo(new CommandInfo());
        main.run(args);
    }

    public Main() {
        jc = new JCommander(this);
        jc.setProgramName("minimesos");
    }

    public void run(String[] args) {
        jc.addCommand(CommandUp.CLINAME, commandUp);
        jc.addCommand(CommandDestroy.CLINAME, commandDestroy);
        jc.addCommand(CommandHelp.CLINAME, commandHelp);
        jc.addCommand(CommandInfo.CLINAME, commandInfo);
        jc.addCommand(CommandInstall.CLINAME, commandInstall);
        jc.addCommand(CommandState.CLINAME, commandState);

        try {
            jc.parse(args);
        } catch (Exception e) {
            LOGGER.error("Failed to parse parameters. " + e.getMessage() + "\n" );
            jc.usage();
            return;
        }

        if (jc.getParameters().get(0).isAssigned()) {
            jc.usage();
            return;
        }

        if (jc.getParsedCommand() == null) {
            MesosCluster cluster = ClusterRepository.loadCluster();
            if (cluster != null) {
                cluster.printServiceUrl("master", commandUp);
                cluster.printServiceUrl("marathon", commandUp);
            } else {
                jc.usage();
            }
            return;
        }

        switch (jc.getParsedCommand()) {
            case CommandHelp.CLINAME:
                jc.usage();
                break;
            case CommandUp.CLINAME:
                commandUp.execute();
                break;
            case CommandDestroy.CLINAME:
                commandDestroy.execute();
                break;
            case CommandInfo.CLINAME:
                commandInfo.execute();
                break;
            case CommandInstall.CLINAME:
                commandInstall.execute();
                break;
            case CommandState.CLINAME:
                commandState.execute();
                break;
            default:
                LOGGER.error("No such command: " + jc.getParsedCommand());
        }


    }

    public void setCommandUp(CommandUp commandUp) {
        this.commandUp = commandUp;
    }

    public void setCommandDestroy(CommandDestroy commandDestroy) {
        this.commandDestroy = commandDestroy;
    }

    public void setCommandHelp(CommandHelp commandHelp) {
        this.commandHelp = commandHelp;
    }

    public void setCommandInstall(CommandInstall commandInstall) {
        this.commandInstall = commandInstall;
    }

    public void setCommandState(CommandState commandState) {
        this.commandState = commandState;
    }

    public void setCommandInfo(CommandInfo commandInfo) {
        this.commandInfo = commandInfo;
    }

}
