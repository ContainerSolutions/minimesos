package com.containersol.minimesos.main;

import java.io.PrintStream;
import java.net.URI;
import java.util.List;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.ClusterUtil;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import com.containersol.minimesos.util.Environment;

/**
 * Info command
 */
@Parameters(separators = "=", commandDescription = "Display cluster information")
public class CommandInfo implements Command {

    public static final String CLINAME = "info";

    private PrintStream output = System.out; //NOSONAR

    private ClusterRepository repository = new ClusterRepository();

    public CommandInfo() { //NOSONAR
    }

    public CommandInfo(PrintStream ps) {
        this.output = ps;
    }

    @Override
    public void execute() {
        String clusterId = repository.readClusterId();
        if (clusterId != null) {
            MesosCluster cluster = repository.loadCluster(new MesosClusterContainersFactory());
            if (cluster != null) {
                output.println("Minimesos cluster is running: " + cluster.getClusterId());
                output.println("Mesos version: " + cluster.getMaster().getState().getVersion());
                printServiceUrls(cluster);
            } else {
                output.println(String.format("Minimesos cluster %s is not running. %s is removed", clusterId, repository.getMinimesosFile().getAbsolutePath()));
            }
        } else {
            output.println("Cluster ID is not found in " + repository.getMinimesosFile().getAbsolutePath());
        }
    }

    /**
     * Prints cluster services URLs and IPs
     *
     * @param cluster to examine
     */
    private void printServiceUrls(MesosCluster cluster) {

        // print independent from roles variables
        String masterContainer = cluster.getMaster().getContainerId();
        String gateway = String.format("export %s=%s", MesosCluster.TOKEN_NETWORK_GATEWAY, DockerContainersUtil.getGatewayIpAddress(masterContainer));
        output.println(gateway);

        List<ClusterProcess> uniqueMembers = ClusterUtil.getDistinctRoleProcesses(cluster.getMemberProcesses());
        for (ClusterProcess process : uniqueMembers) {

            URI serviceUrl = process.getServiceUrl();
            if (serviceUrl != null) {
                String service = String.format("export %s%s=%s", MesosCluster.MINIMESOS_TOKEN_PREFIX, process.getRole().toUpperCase(), serviceUrl.toString());
                String serviceIp = String.format("export %s%s_IP=%s", MesosCluster.MINIMESOS_TOKEN_PREFIX, process.getRole().toUpperCase(), serviceUrl.getHost());

                output.println(String.format("%s; %s", service, serviceIp));
            }

        }

        if (Environment.isRunningInDockerOnMac()) {
            output.println("You are running Docker on Mac so use localhost instead of container IPs for Master, Marathon, Zookeepr and Consul");
        }
    }

    @Override
    public boolean validateParameters() {
        return true;
    }

    @Override
    public String getName() {
        return CLINAME;
    }

}
