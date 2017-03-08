package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

import java.io.PrintStream;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Uninstalls a Marathon app or framework
 */
@Parameters(separators = "=", commandDescription = "Uninstall a Marathon app")
public class CommandUninstall implements Command {

    @Parameter(names = "--app", description = "Marathon app to uninstall")
    private String app = null;

    @Parameter(names = "--group", description = "Marathon group to uninstall")
    private String group = null;

    private ClusterRepository repository = new ClusterRepository();

    private PrintStream output = System.out; // NOSONAR

    CommandUninstall(PrintStream output) {
        this.output = output;
    }

    CommandUninstall() {
        // NOSONAR
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return "uninstall";
    }

    @Override
    public void execute() {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());

        if (cluster == null) {
            output.println("Minimesos cluster is not running");
            return;
        }

        Marathon marathon = cluster.getMarathon();
        if (marathon == null) {
            throw new MinimesosException("Marathon container is not found in cluster " + cluster.getClusterId());
        }

        if (isNotBlank(app) && isNotBlank(group)) {
            output.println("Please specify --app or --group to uninstall an app or group");
            return;
        }

        if (isNotBlank(app)) {
            try {
                marathon.deleteApp(app);
                output.println("Deleted app '" + app + "'");
            } catch (MinimesosException e) { // NOSONAR
                output.println(e.getMessage());
            }
        } else if (isNotBlank(group)) {
            try {
                marathon.deleteGroup(group);
                output.println("Deleted group '" + group + "'");
            } catch (MinimesosException e) { // NOSONAR
                output.println(e.getMessage());
            }
        } else {
            output.println("Please specify --app or --group to uninstall an app or group");
        }
    }

    public void setRepository(ClusterRepository repository) {
        this.repository = repository;
    }

    void setApp(String app) {
        this.app = app;
    }

    void setGroup(String group) {
        this.group = group;
    }
}
