package com.containersol.minimesos.api;

import com.containersol.minimesos.cluster.ClusterRepository;
import com.containersol.minimesos.cluster.MesosCluster;
import com.containersol.minimesos.config.ClusterConfig;
import com.containersol.minimesos.config.ConfigParser;
import com.containersol.minimesos.mesos.MesosClusterContainersFactory;
import spark.Spark;

import java.net.UnknownHostException;

/**
 * minimesos API server
 */
public class ApiServer {

    public static final int PORT = 8080;

    private ClusterRepository repository;

    private final MesosClusterContainersFactory factory;

    private MesosCluster mesosCluster;

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
            if (mesosCluster != null) {
                return new ClusterStartedResponse(mesosCluster.getClusterId());
            } else {
                ClusterConfig clusterConfig = new ConfigParser().parse(request.body());
                mesosCluster = factory.createMesosCluster(clusterConfig);
                mesosCluster.start();
                return new ClusterStartedResponse(mesosCluster.getClusterId());
            }
        }, JsonUtils.json());

        Spark.get("/info", (req, res) -> "No cluster is running");

        Spark.exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
        });
    }

    public void stop() {
        if (mesosCluster != null) {
            mesosCluster.destroy(factory);
        }

        Spark.stop();
    }

    public String getServiceUrl() {
        return "http://localhost:" + PORT;
    }
}
