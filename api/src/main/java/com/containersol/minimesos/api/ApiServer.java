package com.containersol.minimesos.api;

import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import spark.Spark;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * minimesos API server
 */
public class ApiServer {

    public static final int PORT = 8080;

    private ClusterRepository repository;

    private final MesosClusterContainersFactory factory;

    public ApiServer() {
        repository = new ClusterRepository();
        factory = new MesosClusterContainersFactory();
    }

    public static void main(String[] args) throws UnknownHostException {
        ApiServer apiServer = new ApiServer();
        apiServer.start();
    }

    public void start() {
        Spark.port(PORT);

        Spark.post("/start", "text/plain", (request, response) -> {
            ClusterConfig clusterConfig = new ConfigParser().parse(request.body());
            MesosCluster mesosCluster = factory.createMesosCluster(clusterConfig);
            mesosCluster.start();
            return new ClusterStartedResponse(mesosCluster.getClusterId());
        }, JsonUtils.json());

        Spark.get("/info", (req, res) -> "No cluster is running");

        Spark.exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
        });
    }

    public void stop() {
        Spark.stop();
    }

    public String getServiceUrl() {
        try {
            return "http://" + Inet4Address.getLocalHost().getHostAddress() + ":" + PORT;
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not determine IP address: " + e.getMessage());
        }
    }
}
