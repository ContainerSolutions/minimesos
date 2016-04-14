package com.containersol.minimesos.main;

import com.beust.jcommander.Parameters;
import com.containersol.minimesos.cluster.ClusterProcess;
import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ConsulConfig;
import com.containersol.minimesos.config.MarathonConfig;
import com.containersol.minimesos.config.MesosMasterConfig;
import com.containersol.minimesos.config.ZooKeeperConfig;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import org.apache.commons.lang.StringUtils;

import java.io.PrintStream;

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
            printServiceUrls(cluster, output);
        } else {
            output.println("Minimesos cluster is not running");
        }
    }

    public void printServiceUrls(MesosCluster cluster, PrintStream out) {
        boolean exposedHostPorts = cluster.isExposedHostPorts();
        String dockerHostIp = System.getenv("DOCKER_HOST_IP");

        for (ClusterProcess process : cluster.getMemberProcesses()) {
            String ip;
            if (!exposedHostPorts || StringUtils.isEmpty(dockerHostIp)) {
                ip = process.getIpAddress();
            } else {
                ip = dockerHostIp;
            }

            switch (process.getRole()) {
                case "master":
                    out.println("export MINIMESOS_MASTER=http://" + ip + ":" + MesosMasterConfig.MESOS_MASTER_PORT);
                    break;
                case "marathon":
                    out.println("export MINIMESOS_MARATHON=http://" + ip + ":" + MarathonConfig.MARATHON_PORT);
                    break;
                case "zookeeper":
                    out.println("export MINIMESOS_ZOOKEEPER=" + getFormattedZKAddress(ip));
                    break;
                case "consul":
                    out.println("export MINIMESOS_CONSUL=http://" + ip + ":" + ConsulConfig.CONSUL_HTTP_PORT);
                    out.println("export MINIMESOS_CONSUL_IP=" + ip);
                    break;
            }
        }
    }

    /**
     * @param ipAddress overwrites real IP of ZooKeeper container
     * @return ZooKeeper URL based on given IP address
     */
    public static String getFormattedZKAddress(String ipAddress) {
        return "zk://" + ipAddress + ":" + ZooKeeperConfig.DEFAULT_ZOOKEEPER_PORT;
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
