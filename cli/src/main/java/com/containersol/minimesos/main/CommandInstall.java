package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Installs a framework with Marathon
 */
@Parameters(commandDescription = "Install a framework with Marathon")
public class CommandInstall implements Command {

    public static final String CLINAME = "install";

    @Parameter(names = "--marathonFile", description = "Marathon JSON app install file path or URL. Either this or --stdin parameter must be used")
    private String marathonFile = null;

    @Parameter(names = "--stdin", description = "Use JSON from standard import. Allow piping JSON from other processes. Either this or --marathonFile parameter must be used")
    private boolean stdin = false;

    @Parameter(names = "--update", description = "Update a running application instead of attempting to deploy a new application")
    private boolean update = false;

    private ClusterRepository repository = new ClusterRepository();

    /**
     * Getting content of <code>marathonFile</code>, if provided, or standard input
     *
     * @return content of the file or standard input
     */
    public String getMarathonJson() throws IOException {

        String fileContents = "";
        Scanner scanner;

        if (marathonFile != null && !marathonFile.isEmpty()) {

            InputStream json = MesosCluster.getInputStream(marathonFile);
            if (json == null) {
                throw new MinimesosException("Failed to find content of " + marathonFile);
            }

            scanner = new Scanner(json);

        } else if (stdin) {
            scanner = new Scanner(System.in);
        } else {
            throw new MinimesosException("Neither --marathonFile nor --stdin parameters are provided");
        }

        try {
            while (scanner.hasNextLine()) {
                fileContents = fileContents.concat(scanner.nextLine());
            }
        } finally {
            scanner.close();
        }

        return fileContents;
    }

    @Override
    public void execute() {
        MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());
        if (cluster != null) {
            Marathon marathon = cluster.getMarathon();
            if (marathon == null) {
                throw new MinimesosException("Marathon container is not found in cluster " + cluster.getClusterId());
            }

            String marathonJson;
            try {
                marathonJson = getMarathonJson();
            } catch (IOException e) {
                throw new MinimesosException("Failed to read JSON", e);
            }

            if (update) {
                marathon.updateApp(marathonJson);
            } else {
                marathon.deployApp(marathonJson);
            }
        } else {
            throw new MinimesosException("Running cluster is not found");
        }
    }

    @Override
    public boolean validateParameters() {
        return stdin || (marathonFile != null && !marathonFile.isEmpty());
    }

    @Override
    public String getName() {
        return CLINAME;
    }

    public void setMarathonFile(String marathonFile) {
        this.marathonFile = marathonFile;
    }
}
