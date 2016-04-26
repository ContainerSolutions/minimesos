package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;

import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.containersol.minimesos.cluster.Filter.withRole;

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

        List<ClusterProcess> uniqueMembers = getDistinctRoleProcesses(cluster.getMemberProcesses());

        for (ClusterProcess process : uniqueMembers) {

            String processIp = process.getIpAddress();

            URI serviceUrl = process.getServiceUrl();
            if (serviceUrl != null) {
                String service = String.format("export MINIMESOS_%s=%s", process.getRole().toUpperCase(), serviceUrl.toString());
                String serviceIp = String.format("export MINIMESOS_%s_IP=%s", process.getRole().toUpperCase(), processIp);

                output.println(service);
                output.println(serviceIp);
            }

        }

    }

    /**
     * Filters given list of processes and returns only those with distinct roles
     *
     * @param processes complete list of processes
     * @return processes with distinct roles
     */
    public List<ClusterProcess> getDistinctRoleProcesses(List<ClusterProcess> processes) {

        List<ClusterProcess> distinct = new ArrayList<>();
        Map<String, Integer> roles = new HashMap<>();

        // count processes per role
        for (ClusterProcess process : processes) {
            Integer prev = roles.get(process.getRole());
            int count = (prev != null) ? prev : 0;
            roles.put(process.getRole(), count+1 );
        }

        for (Map.Entry<String, Integer> role : roles.entrySet() ) {
            if (role.getValue() == 1) {
                Optional<ClusterProcess> process = processes.stream().filter(withRole(role.getKey())).findFirst();
                distinct.add(process.get());
            }
        }

        return distinct;

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
