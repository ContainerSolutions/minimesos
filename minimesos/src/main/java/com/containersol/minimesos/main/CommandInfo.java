package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.ClusterUtil;
import com.containersol.minimesos.cluster.MesosCluster;
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
        MesosCluster cluster = ClusterRepository.loadCluster(new MesosClusterContainersFactory());
        if (cluster != null) {
            output.println("Minimesos cluster is running: " + cluster.getClusterId());
            if (cluster.getMesosVersion() != null) {
                output.println("Mesos version: " + cluster.getMesosVersion());
            }
            printServiceUrls(cluster);
        } else {
            output.println("Minimesos cluster is not running");
        }
    }

    /**
     * Prints cluster services URLs and IPs
     *
     * @param cluster to examine
     */
    private void printServiceUrls(MesosCluster cluster) {

        List<ClusterProcess> uniqueMembers = ClusterUtil.getDistinctRoleProcesses(cluster.getMemberProcesses());

        for (ClusterProcess process : uniqueMembers) {

            URI serviceUrl = process.getServiceUrl();
            if (serviceUrl != null) {
                String service = String.format("export MINIMESOS_%s=%s", process.getRole().toUpperCase(), serviceUrl.toString());
                String serviceIp = String.format("export MINIMESOS_%s_IP=%s", process.getRole().toUpperCase(), serviceUrl.getHost());

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
