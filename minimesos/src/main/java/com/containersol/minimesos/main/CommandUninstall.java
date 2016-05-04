package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import java.io.PrintStream;

/**
 * Uninstalls a Marathon app or framework
 */
@Parameters(separators = "=", commandDescription = "Uninstall a Marathon app")
public class CommandUninstall implements Command {

    @Parameter(names = "--app", description = "Marathon app to uninstall", required = true)
    private String app = null;

    private ClusterRepository repository = new ClusterRepository();

    private PrintStream output = System.out; // NOSONAR

    public CommandUninstall(PrintStream output) {
        this.output = output;
    }

    public CommandUninstall() {
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
    public void execute() throws MinimesosException {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());

        if (cluster == null) {
            output.println("Minimesos cluster is not running");
            return;
        }

        Marathon marathon = cluster.getMarathon();
        if (marathon == null) {
            throw new MinimesosException("Marathon container is not found in cluster " + cluster.getClusterId());
        }

        try {
            marathon.deleteApp(app);
        } catch (MinimesosException e) {
            return;
        }
        output.println("Deleted app '" + app + "'");
    }

    public void setRepository(ClusterRepository repository) {
        this.repository = repository;
    }

    public void setApp(String app) {
        this.app = app;
    }
}
