package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.ClusterUtil;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.docker.DockerContainersUtil;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

import java.io.PrintStream;
import java.net.URI;
import java.util.List;

/**
 * Info command
 */
@Parameters(separators = "=", commandDescription = "Display cluster information")
public class CommandInfo implements Command {

    public static final String CLINAME = "info";

    private PrintStream output = System.out;

    public CommandInfo() {
    }

    public CommandInfo(PrintStream ps) {
        this.output = ps;
    }

    @Override
    public void execute() {
        String clusterId = ClusterRepository.readClusterId();
        if (clusterId != null) {
            MesosCluster cluster = ClusterRepository.loadCluster(new MesosClusterContainersFactory());
            if (cluster != null) {
                output.println("Minimesos cluster is running: " + cluster.getClusterId());
                if (cluster.getMesosVersion() != null) {
                    output.println("Mesos version: " + cluster.getMesosVersion());
                }
                printServiceUrls(cluster);
            } else {
                output.println(String.format("Minimesos cluster %s is not running. %s is removed", clusterId, ClusterRepository.getMinimesosFile().getAbsolutePath()));
            }
        } else {
            output.println("Cluster ID is not found in " + ClusterRepository.getMinimesosFile().getAbsolutePath());
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
