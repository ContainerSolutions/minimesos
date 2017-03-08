package com.containersol.minimesos.main;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.containersol.minimesos.MinimesosException;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.Marathon;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.*;

/**
 * Installs an Marathon application or application group.
 */
@Parameters(commandDescription = "Install a Marathon application or application group")
public class CommandInstall implements Command {

    private static final String CLINAME = "install";

    @Deprecated
    @Parameter(names = "--marathonFile", description = "[Deprecated - Please use --marathonApp] Relative path or URL to a JSON file with a Marathon app definition.")
    String marathonFile = null;

    @Parameter(names = "--app", description = "Relative path or URL to a JSON file with a Marathon app definition. See https://mesosphere.github.io/marathon/docs/application-basics.html.")
    String app = null;

    @Parameter(names = "--group", description = "Relative path or URL to a JSON file with a group of Marathon apps. See https://mesosphere.github.io/marathon/docs/application-groups.html.")
    String group = null;

    @Parameter(names = "--stdin", description = "Read JSON file with Marathon app or group definition from stdin.")
    private boolean stdin = false;

    @Parameter(names = "--update", description = "Update a running application instead of attempting to deploy a new application")
    private boolean update = false;

    ClusterRepository repository = new ClusterRepository();

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
                throw new MinimesosException("Failed to read JSON file from path, URL or stdin", e);
            }

            if (update) {
                marathon.updateApp(marathonJson);
            } else if (isNotBlank(app) || isNotBlank(marathonFile)) {
                marathon.deployApp(marathonJson);
            } else if (isNotBlank(group)) {
                marathon.deployGroup(marathonJson);
            } else {
                throw new MinimesosException("Neither app, group, --stdinApp or --stdinGroup is provided");
            }
        } else {
            throw new MinimesosException("Running cluster is not found");
        }
    }

    /**
     * Getting content of the Marathon JSON file if specified or via standard input
     *
     * @return content of the file or standard input
     */
    private String getMarathonJson() throws IOException {
        if (stdin) {
            return IOUtils.toString(System.in, "UTF-8");
        } else {
            if (isNotBlank(marathonFile)) {
                return IOUtils.toString(MesosCluster.getInputStream(marathonFile), "UTF-8");
            } else if (isNotBlank(app)) {
                return IOUtils.toString(MesosCluster.getInputStream(app), "UTF-8");
            } else if (isNotBlank(group)) {
                return IOUtils.toString(MesosCluster.getInputStream(group), "UTF-8");
            }
        }
        throw new IOException("Please specify a URL or path to Marathon JSON file or use --stdin");
    }

    @Override
    public boolean validateParameters() {
        return isNotBlank(app) || isNotBlank(group) || isNotBlank(marathonFile);
    }

    @Override
    public String getName() {
        return CLINAME;
    }

}
